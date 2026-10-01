package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionExerciseEditTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `today-only add and remove keep order, current exercise and program intact`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val ids = listOf("Pulldown", "Row", "Pullover", "Curl").map { dao.insertExercise(Exercise(name = it, muscleGroup = "Back")) }
            val program = dao.insertProgram(Program(name = "Split"))
            val day = dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 1, dayCode = "TUE", title = "Back"))
            dao.insertProgramExercise(ProgramExercise(programDayId = day, exerciseId = ids[0], orderIndex = 0))
            val session = dao.insertWorkoutSession(WorkoutSession(title = "Back", programDayId = day, currentExerciseIndex = 1))
            val first = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = session, exerciseId = ids[0], orderIndex = 0))
            val second = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = session, exerciseId = ids[1], orderIndex = 1))
            val third = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = session, exerciseId = ids[2], orderIndex = 2))
            fun order() = runBlocking { dao.getWorkoutDetailsSync(session).map { it.workoutExercise.id } }
            fun current() = runBlocking { dao.getSessionSync(session)!!.currentExerciseIndex }

            // Insert after the current exercise (index 1): the user stays on it.
            val added = dao.addExerciseToActiveSession(session, ids[3], position = 2)
            assertEquals(listOf(first, second, added, third), order())
            assertEquals(1, current())
            assertEquals(listOf(0, 1, 2, 3), dao.getWorkoutDetailsSync(session).map { it.workoutExercise.orderIndex })

            // Removing an earlier exercise shifts the current index; its sets and segments go with it.
            val set = dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = first, setNumber = 1, weightKg = 70f, reps = 10))
            dao.insertSetSegment(SetSegment(workoutSetId = set, segmentIndex = 1, weightKg = 50f, reps = 6))
            val removed = dao.removeExerciseFromActiveSession(session, first)
            assertEquals(listOf(second, added, third), order())
            assertEquals(0, current())
            assertTrue(dao.getAllWorkoutSetsSync().none { it.id == set })
            assertTrue(dao.getAllSetSegmentsSync().isEmpty())

            // Undo restores ids, position, sets, segments and the current exercise.
            dao.restoreRemovedExercise(removed)
            assertEquals(listOf(first, second, added, third), order())
            assertEquals(1, current())
            assertEquals(listOf(set), dao.getAllWorkoutSetsSync().map { it.id })
            assertEquals(1, dao.getAllSetSegmentsSync().size)

            // Removing the current last exercise moves to the new last one.
            dao.selectExercise(session, 3)
            dao.removeExerciseFromActiveSession(session, third)
            assertEquals(2, current())

            // The program is never touched.
            assertEquals(listOf(ids[0]), dao.getAllProgramExercisesSync().map { it.exerciseId })
        } finally {
            db.close()
        }
    }

    @Test
    fun `the last exercise cannot be removed and finished sessions are read only`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Press", muscleGroup = "Chest"))
            val active = dao.insertWorkoutSession(WorkoutSession(title = "Active"))
            val only = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = active, exerciseId = exercise, orderIndex = 0))
            assertTrue(runCatching { dao.removeExerciseFromActiveSession(active, only) }.isFailure)
            assertEquals(1, dao.getWorkoutDetailsSync(active).size)

            val finished = dao.insertWorkoutSession(WorkoutSession(title = "Done", finishedAt = 1L))
            assertTrue(runCatching { dao.addExerciseToActiveSession(finished, exercise, 0) }.isFailure)
            assertTrue(dao.getWorkoutDetailsSync(finished).isEmpty())
        } finally {
            db.close()
        }
    }
}
