package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.model.*
import com.example.ui.screens.AddSessionExerciseSheet
import com.example.ui.screens.SessionExerciseList
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ActiveWorkoutUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-mdpi")
class SessionExerciseListTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val names = listOf("Lat Pulldown", "Chest Supported Row", "Shrug")
    private val state = ActiveWorkoutUiState(
        isLoading = false,
        session = WorkoutSession(id = 1, title = "Back", currentExerciseIndex = 1),
        currentExerciseIndex = 1,
        exercises = names.mapIndexed { i, name ->
            WorkoutExerciseDetail(
                WorkoutExercise(id = i + 1L, workoutSessionId = 1, exerciseId = i + 1L, orderIndex = i),
                Exercise(id = i + 1L, name = name, muscleGroup = "Back"),
                if (i == 0) listOf(WorkoutSet(id = 10, workoutExerciseId = 1, setNumber = 1, weightKg = 70f, reps = 10)) else emptyList()
            )
        }
    )

    @Test fun `tap jumps, delete and swipe request removal, add opens the sheet`() {
        val picked = mutableListOf<Int>()
        val removed = mutableListOf<Long>()
        var adds = 0
        compose.setContent {
            MyApplicationTheme {
                SessionExerciseList(state, enabled = true, onBack = {}, onPick = { picked += it },
                    onRemove = { removed += it.workoutExercise.id }, onAdd = { adds++ })
            }
        }
        compose.onNodeWithText("CURRENT").assertExists()
        compose.onNodeWithText("IN PROGRESS").assertExists()
        compose.onNodeWithText("Shrug").performClick()
        assertEquals(listOf(2), picked)
        compose.onNodeWithTag("session_list_delete_0").performClick()
        assertEquals(listOf(1L), removed)
        compose.onNodeWithTag("session_list_item_2").performTouchInput { swipeLeft(startX = right - 4f, endX = left, durationMillis = 300) }
        compose.waitForIdle()
        assertEquals(listOf(1L, 3L), removed)
        compose.onNodeWithTag("session_add_exercise").performScrollTo().performClick()
        assertEquals(1, adds)
    }

    @Test fun `disabled list ignores taps and removals`() {
        val removed = mutableListOf<Long>()
        compose.setContent {
            MyApplicationTheme {
                SessionExerciseList(state, enabled = false, onBack = {}, onPick = {}, onRemove = { removed += it.workoutExercise.id }, onAdd = {})
            }
        }
        compose.onNodeWithTag("session_list_delete_0").assertIsNotEnabled()
        compose.onNodeWithTag("session_add_exercise").assertIsNotEnabled()
    }

    @Test fun `add sheet hides exercises already in the session and passes the insert position`() {
        val library = listOf(
            Exercise(id = 1, name = "Lat Pulldown", muscleGroup = "Back"),
            Exercise(id = 7, name = "Face Pull", muscleGroup = "Rear delts", manufacturer = "Cable"),
            Exercise(id = 8, name = "Machine Curl", muscleGroup = "Biceps")
        )
        val added = mutableListOf<Pair<Long, Boolean>>()
        compose.setContent {
            MyApplicationTheme {
                AddSessionExerciseSheet(library, sessionExerciseIds = setOf(1L), currentLabel = "Row",
                    onAdd = { ex, after -> added += ex.id to after }, onDismiss = {})
            }
        }
        compose.onNodeWithTag("add_exercise_option_1").assertDoesNotExist()
        compose.onNodeWithTag("add_exercise_search").performTextInput("cable")
        compose.onNodeWithTag("add_exercise_option_8").assertDoesNotExist()
        compose.onNodeWithTag("add_exercise_option_7").performClick()
        compose.onNodeWithTag("add_exercise_search").performTextClearance()
        compose.onNodeWithTag("add_position_end").performClick()
        compose.onNodeWithTag("add_exercise_option_8").performScrollTo().performClick()
        assertEquals(listOf(7L to true, 8L to false), added)
    }
}
