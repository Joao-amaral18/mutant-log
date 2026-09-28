package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DiscardWorkoutTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `discard cascades all session data and survives reopening`() = runBlocking {
        val name = "discard-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, MutantDatabase::class.java, name).build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Press", muscleGroup = "Chest"))
            val finished = dao.insertWorkoutSession(WorkoutSession(title = "Finished", finishedAt = 1L))
            val active = dao.insertWorkoutSession(WorkoutSession(title = "Active", notes = "Session note"))
            val we = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = active, exerciseId = exercise, orderIndex = 1, notes = "Exercise note"))
            val set = dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = we, setNumber = 1, weightKg = 100f, reps = 10, isPr = true))
            dao.insertSetSegment(SetSegment(workoutSetId = set, segmentIndex = 1, weightKg = 80f, reps = 5))
            dao.insertCardioSession(CardioSession(workoutSessionId = active))
            dao.insertCardioSession(CardioSession(notes = "Independent cardio"))
            assertTrue(dao.discardWorkoutSession(active))
            assertFalse(dao.discardWorkoutSession(active))
            assertFalse(dao.discardWorkoutSession(finished))
            assertNull(dao.getActiveWorkoutSession().first())
            assertEquals(listOf(finished), dao.getFinishedWorkoutSessions().first().map { it.id })
            assertTrue(dao.getAllWorkoutExercisesSync().isEmpty())
            assertTrue(dao.getAllWorkoutSetsSync().isEmpty())
            assertTrue(dao.getAllSetSegmentsSync().isEmpty())
            assertTrue(dao.getAllSetsForExercise(exercise).first().isEmpty())
            assertTrue(dao.getWorkSetsSince(0L).first().isEmpty())
            assertEquals(1, dao.countCardio())
            db.close()
            db = Room.databaseBuilder(context, MutantDatabase::class.java, name).build()
            assertNull(db.mutantDao().getActiveWorkoutSession().first())
            assertNull(db.mutantDao().getWorkoutSessionById(active).first())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `discard cleanup clears persisted timer and notifications`() {
        val prefs = context.getSharedPreferences(com.example.ui.components.RestTimerAlerts.PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(com.example.ui.components.WorkoutTimerService.KEY_REMAINING, 180)
            .putBoolean(com.example.ui.components.WorkoutTimerService.KEY_RUNNING, true)
            .putLong(com.example.ui.components.WorkoutTimerService.KEY_DEADLINE, Long.MAX_VALUE)
            .putString(com.example.ui.components.WorkoutTimerService.KEY_EXERCISE, "Press")
            .putString(com.example.ui.components.WorkoutTimerService.KEY_SET, "Set 2")
            .putBoolean(com.example.ui.components.RestTimerAlerts.SOUND_ENABLED_KEY, true)
            .commit()
        com.example.ui.components.RestTimerAlerts.prepare(context)
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val notification = android.app.Notification.Builder(context, "rest_timer_running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Rest").build()
        manager.notify(4100, notification)
        manager.notify(4101, notification)
        assertEquals(2, manager.activeNotifications.size)
        com.example.ui.components.RestTimerAlerts.clearSession(context)
        assertFalse(prefs.contains(com.example.ui.components.WorkoutTimerService.KEY_REMAINING))
        assertFalse(prefs.contains(com.example.ui.components.WorkoutTimerService.KEY_RUNNING))
        assertFalse(prefs.contains(com.example.ui.components.WorkoutTimerService.KEY_DEADLINE))
        assertFalse(prefs.contains(com.example.ui.components.WorkoutTimerService.KEY_EXERCISE))
        assertFalse(prefs.contains(com.example.ui.components.WorkoutTimerService.KEY_SET))
        assertTrue(prefs.getBoolean(com.example.ui.components.RestTimerAlerts.SOUND_ENABLED_KEY, false))
        assertTrue(context.getSystemService(android.app.NotificationManager::class.java).activeNotifications.isEmpty())
    }

    @Test
    fun `failure rolls back session and cascaded data`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Press", muscleGroup = "Chest"))
            val active = dao.insertWorkoutSession(WorkoutSession(title = "Active"))
            val we = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = active, exerciseId = exercise, orderIndex = 1))
            dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = we, setNumber = 1, weightKg = 50f, reps = 10))
            dao.insertCardioSession(CardioSession(workoutSessionId = active))
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_cardio_delete BEFORE DELETE ON cardio_sessions BEGIN SELECT RAISE(ABORT, 'Injected failure'); END")
            var failed = false
            try { dao.discardWorkoutSession(active) } catch (e: Exception) { failed = true }
            assertTrue(failed)
            assertEquals(active, dao.getActiveWorkoutSession().first()?.id)
            assertEquals(1, dao.getAllWorkoutExercisesSync().size)
            assertEquals(1, dao.countSets())
            assertEquals(1, dao.countCardio())
        } finally { db.close() }
    }
}

