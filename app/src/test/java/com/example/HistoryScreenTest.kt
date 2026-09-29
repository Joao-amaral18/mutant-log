package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.model.*
import com.example.ui.screens.HistoryContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun fixture(): HistoryUiState {
        val now = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 18); set(Calendar.MINUTE, 42) }.timeInMillis
        return HistoryUiState(loading = false, workouts = (0..2).map { index ->
            val id = index + 1L
            HistoryWorkout(WorkoutSession(id = id, title = if (index == 1) "Push" else "Pull", startedAt = now - index * 86_400_000L, finishedAt = now - index * 86_400_000L + 4_020_000, notes = "Boa sessão. Foco na execução."), (0..2).map { ex ->
                val exId = ex + 1L
                HistoryExercise(WorkoutExercise(id = id * 10 + ex, workoutSessionId = id, exerciseId = exId, orderIndex = ex), Exercise(id = exId, name = listOf("Lat Pulldown", "Chest Supported Row", "Barbell Curl")[ex], muscleGroup = "Back"), (1..3).map { set ->
                    HistorySet(WorkoutSet(id = id * 100 + ex * 10 + set, workoutExerciseId = id * 10 + ex, setNumber = set, weightKg = 80f - index * 2.5f - ex * 10, reps = 10 - set + 1, rir = 2, isPr = index == 0 && set == 1, prType = "LOAD"))
                })
            })
        })
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val target = File("build/test-artifacts/history-$name.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `workout filters come from recorded titles and preserve the original sessions`() {
        val base = fixture()
        val titles = listOf("Chest + Biceps", "Chest + Biceps (leve)", "Chest + Biceps")
        val state = base.copy(workouts = base.workouts.mapIndexed { index, workout ->
            workout.copy(session = workout.session.copy(title = titles[index]))
        })
        compose.setContent { MyApplicationTheme { HistoryContent(state) } }
        compose.onNodeWithTag("history_filter_Chest + Biceps").assertExists().performClick()
        compose.onNodeWithTag("history_session_1").performScrollTo().assertExists()
        compose.onNodeWithTag("history_session_2").assertDoesNotExist()
        compose.onNodeWithTag("history_session_3").performScrollTo().assertExists()
        compose.onNodeWithTag("history_filter_Push").assertDoesNotExist()
        compose.onNodeWithTag("history_filter_Pull").assertDoesNotExist()
        compose.onNodeWithTag("history_filter_Legs").assertDoesNotExist()
        capture("recorded-titles")
    }

    @Test fun `normal phone navigates sessions exercises chart and calendar`() {
        var edited: Long? = null
        compose.setContent { MyApplicationTheme { HistoryContent(fixture(), onEdit = { edited = it }) } }
        compose.onNodeWithTag("history_tab_0").assertIsSelected()
        capture("normal-trainings")
        compose.onNodeWithTag("history_session_1").performScrollTo().performClick()
        compose.onNodeWithTag("history_session_detail").assertExists()
        capture("normal-session")
        compose.onNodeWithTag("history_edit").performClick()
        assertEquals(1L, edited)
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("history_tab_1").performClick()
        capture("normal-exercises")
        compose.onNodeWithTag("history_exercise_1").performScrollTo().performClick()
        compose.onNodeWithTag("history_metric_Volume").performClick()
        capture("normal-evolution")
        compose.onNodeWithTag("history_exercise_session_1").performScrollTo().performClick()
        compose.onNodeWithTag("history_session_detail").assertExists()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("history_exercise_detail").assertExists()
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("history_tab_2").performClick()
        capture("normal-calendar")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w320dp-h640dp-mdpi")
    fun `compact phone supports search and calendar day navigation`() {
        val state = fixture()
        compose.setContent { MyApplicationTheme { HistoryContent(state) } }
        capture("compact-trainings")
        compose.onNodeWithTag("history_search").performTextInput("80")
        compose.onNodeWithTag("history_session_1").performScrollTo().performClick()
        capture("compact-session")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("history_tab_1").performClick()
        capture("compact-exercises")
        compose.onNodeWithTag("history_exercise_1").performScrollTo().performClick()
        capture("compact-evolution")
        compose.onNodeWithContentDescription("Voltar").performClick()
        compose.onNodeWithTag("history_tab_2").performClick()
        capture("compact-calendar")
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(state.workouts.first().session.startedAt))
        compose.onNodeWithTag("history_day_$date").performScrollTo().performClick()
        compose.onNodeWithTag("history_session_1").performScrollTo().performClick()
        compose.onNodeWithTag("history_session_detail").assertExists()
    }
}
