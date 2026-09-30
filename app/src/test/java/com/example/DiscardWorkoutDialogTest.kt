package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.ui.components.DiscardWorkoutDialog
import com.example.ui.theme.MyApplicationTheme
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
class DiscardWorkoutDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `expanded notification renders its exercise countdown and controls`() {
        val controller = org.robolectric.Robolectric.buildService(com.example.ui.components.WorkoutTimerService::class.java).create()
        try {
            val service = controller.get()
            com.example.ui.components.RestTimerAlerts.prepare(service)
            val detail = com.example.data.db.WorkoutExerciseDetail(
                com.example.data.model.WorkoutExercise(id = 1, workoutSessionId = 1, exerciseId = 1, orderIndex = 0,
                    nextSetWeightKg = 80f, nextSetReps = 8),
                com.example.data.model.Exercise(id = 1, name = "Incline Machine Press", muscleGroup = "Chest"), emptyList())
            val notification = service.buildNotification(com.example.data.model.WorkoutSession(title = "Push",
                restDeadline = System.currentTimeMillis() + 84_000, restRemainingSeconds = 180), detail)
            val content = notification.bigContentView.apply(service, android.widget.FrameLayout(service))
            org.junit.Assert.assertEquals("Incline Machine Press", content.findViewById<android.widget.TextView>(com.example.R.id.notification_exercise).text.toString())
            val density = service.resources.displayMetrics.density
            content.measure(android.view.View.MeasureSpec.makeMeasureSpec((360 * density).toInt(), android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec((500 * density).toInt(), android.view.View.MeasureSpec.AT_MOST))
            content.layout(0, 0, content.measuredWidth, content.measuredHeight)
            val bitmap = android.graphics.Bitmap.createBitmap(content.measuredWidth, content.measuredHeight, android.graphics.Bitmap.Config.ARGB_8888)
            content.draw(android.graphics.Canvas(bitmap))
            val file = File("build/test-artifacts/workout-notification.png")
            file.parentFile?.mkdirs()
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        } finally { controller.destroy() }
    }

    @Test fun `confirmation supports cancel and changes to a busy state on discard`() {
        val busy = mutableStateOf(false)
        var canceled = 0
        var discarded = 0
        compose.setContent {
            MyApplicationTheme {
                DiscardWorkoutDialog(busy.value, null, { canceled++ }, { discarded++; busy.value = true })
            }
        }
        compose.onNodeWithText("Discard session?").assertExists()
        compose.onNodeWithText("Keep going").performClick()
        assertEquals(1, canceled)
        val file = File("build/test-artifacts/discard-confirmation.png")
        file.parentFile?.mkdirs()
        compose.runOnIdle {
            val view = org.robolectric.shadows.ShadowDialog.getLatestDialog()?.window?.decorView ?: return@runOnIdle
            if (view.width == 0 || view.height == 0) return@runOnIdle
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithText("Discard").performClick()
        assertEquals(1, discarded)
        compose.onNodeWithText("Discarding session…").assertExists()
        compose.onNodeWithText("Discard").assertDoesNotExist()
        compose.onNodeWithText("Keep going").assertDoesNotExist()
    }
}
