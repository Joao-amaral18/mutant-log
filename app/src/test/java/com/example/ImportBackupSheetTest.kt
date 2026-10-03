package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.BackupExport
import com.example.data.repository.ImportMode
import com.example.data.repository.ImportPreview
import com.example.data.repository.ImportResult
import com.example.ui.components.ImportBackupSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ImportUiState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImportBackupSheetTest {
    @get:Rule val compose = createComposeRule()

    private val preview = ImportPreview(
        backup = BackupExport(databaseVersion = 6), legacy = true, generatedAt = "2026-09-30T18:00:00Z",
        workouts = 42, sets = 380, cardio = 9, firstWorkoutAt = 1_780_000_000_000L, lastWorkoutAt = 1_790_000_000_000L,
        warnings = listOf("Older export: program, exercise defaults and notes may be incomplete.")
    )

    private fun capture(name: String) {
        compose.runOnIdle {
            val view = org.robolectric.shadows.ShadowDialog.getLatestDialog()?.window?.decorView ?: return@runOnIdle
            if (view.width == 0 || view.height == 0) return@runOnIdle
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val file = File("build/test-artifacts/import-$name.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `preview defaults to merge and replace asks for a destructive confirmation`() {
        val confirmed = mutableListOf<ImportMode>()
        compose.setContent {
            MyApplicationTheme { ImportBackupSheet(ImportUiState.Preview(preview), { confirmed += it }, {}, {}) }
        }
        compose.onNodeWithText("Import backup").assertExists()
        compose.onNodeWithText("42").assertExists()
        compose.onNodeWithTag("import_warning").assertExists()
        compose.onNodeWithTag("import_mode_merge").assertIsSelected()
        compose.onNodeWithTag("import_replace_warning").assertDoesNotExist()
        capture("preview")

        compose.onNodeWithTag("import_mode_replace").performClick().assertIsSelected()
        compose.onNodeWithTag("import_replace_warning").assertExists()
        compose.onNodeWithText("Replace data").assertExists()
        capture("replace")
        compose.onNodeWithTag("import_confirm_button").performScrollTo().performClick()
        assertEquals(listOf(ImportMode.REPLACE), confirmed)
    }

    @Test fun `done shows the summary and offers the safety backup`() {
        val shared = mutableListOf<File>()
        var closed = 0
        val backup = File("safety.json")
        compose.setContent {
            MyApplicationTheme {
                ImportBackupSheet(
                    ImportUiState.Done(ImportResult(40, 2, 360, 9, 0, 1, 1, 0), ImportMode.REPLACE, backup),
                    {}, { closed++ }, { shared += it }
                )
            }
        }
        compose.onNodeWithText("Data restored").assertExists()
        compose.onNodeWithTag("import_summary")
            .assertTextContains("40 workouts and 360 sets added", substring = true)
            .assertTextContains("2 already on this phone, skipped", substring = true)
        capture("done")
        compose.onNodeWithTag("import_share_backup").performClick()
        assertEquals(listOf(backup), shared)
        compose.onNodeWithTag("import_done").performClick()
        assertEquals(1, closed)
    }

    @Test fun `errors are shown and running cannot be dismissed`() {
        val state = mutableStateOf<ImportUiState>(ImportUiState.Error("This isn't a Mutant Log export, or the file is damaged."))
        compose.setContent { MyApplicationTheme { ImportBackupSheet(state.value, {}, {}, {}) } }
        compose.onNodeWithTag("import_error").assertExists()
        compose.onNodeWithText("isn't a Mutant Log export", substring = true).assertExists()
        state.value = ImportUiState.Running(ImportMode.MERGE)
        compose.onNodeWithTag("import_busy").assertExists()
        compose.onNodeWithText("Merging…").assertExists()
    }
}
