package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import com.example.data.repository.MutantRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutHistoryPersistenceTest {
    @Test fun `transactional history excludes active sessions and observes edits after reopening`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "history-reload-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, MutantDatabase::class.java, name).build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Bench Press", muscleGroup = "Chest"))
            val completed = dao.insertWorkoutSession(WorkoutSession(title = "Push", startedAt = 100, finishedAt = 3_600_100))
            val active = dao.insertWorkoutSession(WorkoutSession(title = "Active"))
            val we = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = completed, exerciseId = exercise, orderIndex = 0))
            dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = active, exerciseId = exercise, orderIndex = 0))
            val setId = dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = we, setNumber = 1, weightKg = 100f, reps = 8))
            dao.insertSetSegment(SetSegment(workoutSetId = setId, segmentIndex = 0, weightKg = 80f, reps = 4))
            dao.insertCardioSession(CardioSession(workoutSessionId = completed, rpe = 7))
            val history = MutantRepository(dao).workoutHistory.first()
            assertEquals(1, history.size)
            assertEquals(1120.0, history.single().volume, 0.001)
            assertEquals(7, history.single().cardio.single().rpe)
            val draft = dao.loadWorkoutDraft(completed)
            val updatedEmission = async { withTimeout(10_000) { MutantRepository(dao).workoutHistory.first { it.single().volume == 1200.0 } } }
            dao.saveWorkoutDraft(draft.copy(exercises = draft.exercises.map { detail -> detail.copy(sets = detail.sets.map { it.copy(weightKg = 110f) }) }))
            assertEquals(1200.0, updatedEmission.await().single().volume, 0.001)
            val changed = MutantRepository(dao).workoutHistory.first().single()
            assertEquals(1200.0, changed.volume, 0.001)
            assertEquals(1, changed.prs)
            db.close()
            db = Room.databaseBuilder(context, MutantDatabase::class.java, name).build()
            assertEquals(changed, MutantRepository(db.mutantDao()).workoutHistory.first().single())
            assertTrue(db.mutantDao().discardWorkoutSession(active))
            assertEquals(listOf(changed), MutantRepository(db.mutantDao()).workoutHistory.first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
