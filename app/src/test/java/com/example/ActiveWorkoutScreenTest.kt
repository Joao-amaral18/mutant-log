package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.db.AD_HOC_TARGET_SETS
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

    private fun fixture(longTitle: Boolean = false, firstDone: Boolean = false, adHocSecond: Boolean = false) = ActiveWorkoutUiState(
        isLoading = false,
        session = WorkoutSession(id = 1, title = if (longTitle) "A long custom hypertrophy workout title" else "Pull Hypertrophy"),
        exercises = listOf("Neutral Lat Pulldown", "T-Bar Chest Supported Row", "Barbell Curl").mapIndexed { index, name ->
            WorkoutExerciseDetail(
                WorkoutExercise(id = index + 1L, workoutSessionId = 1, exerciseId = index + 1L,
                    orderIndex = index, nextSetWeightKg = 60f, nextSetReps = 10,
                    targetWorkSets = if (adHocSecond && index == 1) AD_HOC_TARGET_SETS else null),
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
    private val removedSets = mutableListOf<Long>()
    private var plannedRemovals = 0
    private var addedSets = 0

    private fun render(longTitle: Boolean = false, enabled: Boolean = true, firstDone: Boolean = false,
                       adHocSecond: Boolean = false, startIndex: Int = 0) {
        compose.setContent {
            var state by remember { mutableStateOf(fixture(longTitle, firstDone, adHocSecond).copy(currentExerciseIndex = startIndex)) }
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
                    onLogSet = { logged.add(LoggedSet(type, weight.toFloat(), reps.toInt(), rir, technique)) },
                    onRemoveSet = { removedSets += it },
                    onRemovePlannedSet = { plannedRemovals++ },
                    onAddSet = { addedSets++ }
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

    @Test fun `finished exercise previews what is up next and moves on`() {
        render(firstDone = true)
        compose.onNodeWithTag("log_set_button").assertDoesNotExist()
        compose.onNodeWithTag("up_next_card").assertExists()
        compose.onNodeWithText("UP NEXT · NAUTILUS PRO DUAL PULLEY").assertExists()
        compose.onNodeWithTag("active_workout_content").performScrollToNode(hasTestTag("next_exercise"))
        capture("normal-done")
        compose.onNodeWithTag("add_set").performScrollTo().performClick()
        assertEquals(1, addedSets)
        compose.onNodeWithTag("next_exercise").performScrollTo().performClick()
        assertEquals(1, selectedIndex)
    }

    @Test fun `logged sets swipe away and planned sets trim but never below what is logged`() {
        render(firstDone = true)
        compose.onNodeWithTag("set_row_1").performTouchInput { swipeLeft(startX = right - 4f, endX = left, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals(listOf(1L), removedSets)
        compose.onNodeWithTag("delete_set_row_2").performClick()
        assertEquals(listOf(1L, 2L), removedSets)
        // Two planned sets, both logged: nothing left to trim.
        compose.onNodeWithTag("set_row_pending_1").assertDoesNotExist()
        assertEquals(0, plannedRemovals)
    }

    @Test fun `an unlogged planned set can be dropped for today`() {
        render()
        compose.onNodeWithTag("set_row_pending_1").performTouchInput { swipeLeft(startX = right - 4f, endX = left, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals(1, plannedRemovals)
        compose.onNodeWithText("LAST").assertExists()
        compose.onNodeWithText("Swipe a set left to remove").assertExists()
    }

    @Test fun `exercise added today has no plan and logs open-ended sets`() {
        render(adHocSecond = true, startIndex = 1)
        compose.onNodeWithTag("ad_hoc_chip").assertExists()
        compose.onNodeWithText("Log as you go").assertExists()
        compose.onNodeWithTag("add_set").assertDoesNotExist()
        compose.onNodeWithText("Log as many sets as you need").assertExists()
        compose.onNodeWithTag("log_set_button").performScrollTo().assertTextContains("Log set 1", substring = true)
        compose.onNodeWithTag("ad_hoc_next").assertDoesNotExist()
        capture("normal-adhoc")
    }

    @Test fun `exercise added today offers Done once a set is logged`() {
        compose.setContent {
            val base = fixture(adHocSecond = true)
            val withSet = base.exercises.mapIndexed { i, d ->
                if (i == 1) d.copy(sets = listOf(WorkoutSet(id = 9, workoutExerciseId = 2, setNumber = 1, weightKg = 40f, reps = 12))) else d
            }
            MyApplicationTheme {
                ActiveWorkoutContent(
                    state = base.copy(exercises = withSet, currentExerciseIndex = 1), weightValue = "40", repsValue = "12",
                    setType = SetType.WORK, rir = 1, technique = IntensityTechnique.NONE, seatPosition = "", handlePosition = "",
                    segments = emptyList(), enabled = true, onBack = {}, onFinish = { finished++ },
                    onSelectExercise = { selectedIndex = it }, onSetup = {}, onTarget = {}, onWeightChange = {}, onRepsChange = {},
                    onSetTypeChange = {}, onRirChange = {}, onTechniqueChange = {}, onEditSegments = {}, onLogSet = {}
                )
            }
        }
        compose.onNodeWithTag("log_set_button").performScrollTo().assertTextContains("Log set 2", substring = true)
        compose.onNodeWithTag("ad_hoc_next").performScrollTo().assertTextContains("Done · Next: Barbell Curl", substring = true).performClick()
        assertEquals(2, selectedIndex)
        // Never marked done: the pill keeps its number.
        compose.onNodeWithTag("exercise_progress_1").assertTextContains("2", substring = true)
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
