package com.example

import android.app.Notification
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.model.*
import com.example.ui.components.RestTimerAlerts
import com.example.ui.components.WorkoutTimerService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.LEGACY)
@org.robolectric.annotation.SQLiteMode(org.robolectric.annotation.SQLiteMode.Mode.LEGACY)
class WorkoutNotificationTest {
    @Test fun `rest notification exposes planned set countdown progress and skip`() {
        val controller = Robolectric.buildService(WorkoutTimerService::class.java).create()
        val service = controller.get()
        RestTimerAlerts.prepare(service)
        val detail = WorkoutExerciseDetail(
            WorkoutExercise(id = 1, workoutSessionId = 1, exerciseId = 1, orderIndex = 0,
                nextSetWeightKg = 80f, nextSetReps = 8),
            Exercise(id = 1, name = "Incline press", muscleGroup = "Chest"), emptyList())
        val notification = service.buildNotification(WorkoutSession(id = 1, title = "Push",
            restDeadline = System.currentTimeMillis() + 84_000, restRemainingSeconds = 180), detail)
        assertEquals("Rest \u00b7 Incline press", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Next set: 80 kg \u00d7 8", notification.extras.getString(Notification.EXTRA_TEXT))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertTrue(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN))
        assertEquals(180, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertTrue(notification.extras.getInt(Notification.EXTRA_PROGRESS) in 83..84)
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertNotNull(notification.contentIntent)
        assertTrue(org.robolectric.Shadows.shadowOf(notification.contentIntent).savedIntent.getBooleanExtra(RestTimerAlerts.OPEN_WORKOUT_EXTRA, false))
        val content = notification.bigContentView.apply(service, android.widget.FrameLayout(service))
        assertEquals("Skip rest", content.findViewById<android.widget.TextView>(R.id.notification_label_skip).text.toString())
        assertTrue(content.findViewById<android.widget.Chronometer>(R.id.notification_countdown).isCountDown)
        assertEquals(android.view.View.VISIBLE, content.findViewById<android.view.View>(R.id.notification_action_pause).visibility)
        assertEquals("Finish", content.findViewById<android.widget.TextView>(R.id.notification_label_finish).text.toString())
        val ready = service.buildNotification(WorkoutSession(title = "Push", restCompleted = true), detail)
        assertFalse(ready.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER))
        assertEquals("Ready \u00b7 Incline press", ready.extras.getString(Notification.EXTRA_TITLE))
        assertNull(ready.actions)
        val readyContent = ready.bigContentView.apply(service, android.widget.FrameLayout(service))
        assertEquals(android.view.View.GONE, readyContent.findViewById<android.view.View>(R.id.notification_action_skip).visibility)
        assertEquals(android.view.View.VISIBLE, readyContent.findViewById<android.view.View>(R.id.notification_action_finish).visibility)
        controller.destroy()
    }

    @Test fun `deadline survives reload and skipping clears persisted rest without ending workout`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), MutantDatabase::class.java).build()
        try {
            val dao = db.mutantDao()
            dao.insertWorkoutSession(WorkoutSession(title = "Push"))
            dao.changeRestTimer("start", 180)
            val running = dao.getActiveSessionSync()!!
            assertEquals(96, running.restSecondsAt(running.restDeadline!! - 96_000))
            assertEquals(0, running.restSecondsAt(running.restDeadline + 1_000))
            dao.changeRestTimer("skip")
            val skipped = dao.getActiveSessionSync()!!
            assertNull(skipped.restDeadline)
            assertEquals(0, skipped.restRemainingSeconds)
            assertNull(skipped.finishedAt)
            assertEquals(0, dao.completeRestIfDue(running.id, running.restDeadline, Long.MAX_VALUE))
            assertFalse(dao.getActiveSessionSync()!!.restCompleted)
        } finally { db.close() }
    }
}
