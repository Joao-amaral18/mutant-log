package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.model.*
import com.example.ui.screens.ActiveWorkoutContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveWorkoutUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ActiveWorkoutScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val exercise = Exercise(id = 1, name = "Neutral Lat Pulldown", manufacturer = "Nautilus Pro Dual Pulley",
        muscleGroup = "Back", executionCues = "Keep your chest tall.")

    private fun fixture(longTitle: Boolean = false, firstDone: Boolean = false) = ActiveWorkoutUiState(
        isLoading = false,
        session = WorkoutSession(id = 1, title = if (longTitle) "A long custom hypertrophy workout title" else "Pull Hypertrophy"),
        exercises = listOf("Neutral Lat Pulldown", "T-Bar Chest Supported Row", "Barbell Curl").mapIndexed { index, name ->
            WorkoutExerciseDetail(
                WorkoutExercise(id = index + 1L, workoutSessionId = 1, exerciseId = index + 1L,
                    orderIndex = index, nextSetWeightKg = 60f, nextSetReps = 10),
                exercise.copy(id = index + 1L, name = name),
                if (firstDone && index == 0) (1..2).map { WorkoutSet(id = it.toLong(), workoutExerciseId = 1, setNumber = it, weightKg = 60f, reps = 10, rir = 1) }
                else emptyList()
            )
        }
    )

    private data class LoggedSet(val type: SetType, val weight: Float, val reps: Int, val rir: Int, val technique: IntensityTechnique)
    private val logged = mutableListOf<LoggedSet>()
    private var finished = 0
    private var segmentEdits = 0
    private var selectedIndex = 0

    private fun render(longTitle: Boolean = false, enabled: Boolean = true, firstDone: Boolean = false) {
        compose.setContent {
            var state by remember { mutableStateOf(fixture(longTitle, firstDone)) }
            var weight by remember { mutableStateOf("60.0") }
            var reps by remember { mutableStateOf("10") }
            var type by remember { mutableStateOf(SetType.WORK) }
            var rir by remember { mutableIntStateOf(0) }
            var technique by remember { mutableStateOf(IntensityTechnique.NONE) }
            MyApplicationTheme {
                ActiveWorkoutContent(
                    state = state, weightValue = weight, repsValue = reps, setType = type, rir = rir,
                    technique = technique, seatPosition = "#3 Lock", handlePosition = "MAG Grip Close",
                    segments = emptyList(), enabled = enabled,
                    onBack = {}, onFinish = { finished++ },
                    onSelectExercise = { selectedIndex = it; state = state.copy(currentExerciseIndex = it) },
                    onSetup = {}, onTarget = {},
                    onWeightChange = { weight = it }, onRepsChange = { reps = it },
                    onSetTypeChange = { type = it }, onRirChange = { rir = it },
                    onTechniqueChange = { technique = it }, onEditSegments = { segmentEdits++ },
                    onLogSet = { logged.add(LoggedSet(type, weight.toFloat(), reps.toInt(), rir, technique)) }
                )
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val target = File("build/test-artifacts/workout-$name.png")
            target.parentFile?.mkdirs()
            target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun `normal phone logs the selected metrics and preserves workout actions`() {
        render()
        capture("normal-top")
        compose.onNodeWithTag("weight_plus_button").performScrollTo().performClick()
        compose.onNodeWithTag("reps_minus_button").performScrollTo().performClick()
        compose.onNodeWithTag("rir_option_2").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("technique_PARTIAL_REPS").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("log_set_button").performScrollTo().performClick()
        assertEquals(listOf(LoggedSet(SetType.WORK, 62.5f, 9, 2, IntensityTechnique.PARTIAL_REPS)), logged)
        capture("normal-logger")
        compose.onNodeWithTag("exercise_progress_1").performClick()
        assertEquals(1, selectedIndex)
        compose.onNodeWithTag("exercise_progress_0").performClick()
        assertEquals(0, selectedIndex)
        compose.onNodeWithTag("finish_workout").performClick()
        assertEquals(1, finished)
    }

    @Test fun `finished exercise offers the next exercise and an extra set`() {
        render(firstDone = true)
        compose.onNodeWithTag("log_set_button").assertDoesNotExist()
        compose.onNodeWithTag("active_workout_content").performScrollToNode(hasTestTag("next_exercise"))
        capture("normal-done")
        compose.onNodeWithTag("extra_set").performScrollTo().performClick()
        compose.onNodeWithTag("log_set_button").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("next_exercise").assertDoesNotExist()
        compose.onNodeWithTag("exercise_progress_1").performClick()
        assertEquals(1, selectedIndex)
    }

    @Test
    @Config(sdk = [35], qualifiers = "w320dp-h640dp-mdpi")
    fun `compact phone handles warmups segments and invalid input without hidden actions`() {
        render(longTitle = true)
        capture("compact-top")
        compose.onNodeWithTag("set_type_WARMUP").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("weight_input").performScrollTo().performTextReplacement("0")
        compose.onNodeWithTag("weight_minus_button").performScrollTo().performClick()
        compose.onNodeWithTag("weight_input").assertTextEquals("0")
        compose.onNodeWithTag("reps_input").performScrollTo().performTextReplacement("1")
        compose.onNodeWithTag("reps_minus_button").performScrollTo().performClick()
        compose.onNodeWithTag("reps_input").assertTextEquals("1")
        compose.onNodeWithTag("technique_DROP_SET").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("set_type_WARMUP").assertIsNotSelected()
        compose.onNodeWithTag("edit_set_segments").performScrollTo().performClick()
        assertEquals(1, segmentEdits)
        compose.onNodeWithTag("set_type_WARMUP").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("log_set_button").performScrollTo().performClick()
        assertEquals(SetType.WARMUP, logged.single().type)
        assertEquals(IntensityTechnique.NONE, logged.single().technique)
        capture("compact-logger")
        compose.onNodeWithTag("weight_input").performScrollTo().performTextClearance()
        compose.onNodeWithTag("log_set_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("set_input_error").assertExists()
        compose.onNodeWithTag("weight_input").performScrollTo().performTextReplacement("42,5")
        compose.onNodeWithTag("weight_input").assertTextEquals("42.5")
        compose.onNodeWithTag("log_set_button").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("finish_workout").assertIsDisplayed()
        capture("compact-queue")
    }

    @Test fun `workout mutations are disabled during completion or discard`() {
        render(enabled = false)
        compose.onNodeWithTag("finish_workout").assertIsNotEnabled()
        compose.onNodeWithTag("weight_plus_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("rir_option_0").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("log_set_button").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("exercise_progress_1").assertIsNotEnabled()
    }
}
