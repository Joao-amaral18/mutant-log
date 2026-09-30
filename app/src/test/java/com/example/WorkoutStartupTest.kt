package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.WorkoutSession
import com.example.ui.screens.ActiveWorkoutScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MutantViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutStartupTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `start callback runs only after the workout and exercises are ready`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dao = MutantDatabase.getDatabase(app).mutantDao()
        val exerciseId = runBlocking { dao.insertExercise(com.example.data.model.Exercise(name = "Startup press", muscleGroup = "Chest")) }
        val vm = MutantViewModel(app)
        val store = ViewModelStore().apply { put("test", vm) }
        var ready: com.example.ui.viewmodel.ActiveWorkoutUiState? = null
        try {
            compose.setContent { MyApplicationTheme { ActiveWorkoutScreen(vm, {}) } }
            compose.runOnIdle {
                vm.startAdHocWorkout("Startup test", listOf(exerciseId), com.example.data.repository.ReadinessInput()) {
                    ready = vm.activeWorkoutUiState.value
                }
            }
            try {
                compose.waitUntil(10_000) {
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    ready != null
                }
            } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
                throw AssertionError("Start error: ${vm.startWorkoutError.value}; loading=${vm.activeWorkoutUiState.value.isLoading}; session=${vm.activeWorkoutUiState.value.session?.title}; exercises=${vm.activeWorkoutUiState.value.exercises.size}", e)
            }
            org.junit.Assert.assertFalse(ready!!.isLoading)
            org.junit.Assert.assertEquals("Startup test", ready!!.session!!.title)
            org.junit.Assert.assertEquals(listOf(exerciseId), ready!!.exercises.map { it.exercise.id })
            compose.onNodeWithText("No workout running.").assertDoesNotExist()
        } finally {
            val id = vm.activeWorkoutUiState.value.session?.id
            store.clear()
            if (id != null) runBlocking { dao.discardWorkoutSession(id) }
        }
    }

    @Test fun `active session waiting for exercises never shows the idle session`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = MutantDatabase.getDatabase(app)
        val id = runBlocking { db.mutantDao().insertWorkoutSession(WorkoutSession(title = "Starting")) }
        val vm = MutantViewModel(app)
        val store = ViewModelStore().apply { put("test", vm) }
        try {
            compose.setContent { MyApplicationTheme { ActiveWorkoutScreen(vm, {}) } }
            compose.waitUntil(10_000) { vm.activeWorkoutUiState.value.session?.id == id }
            compose.onNodeWithText("No workout running.").assertDoesNotExist()
        } finally {
            store.clear()
            runBlocking { db.mutantDao().discardWorkoutSession(id) }
        }
    }
}
