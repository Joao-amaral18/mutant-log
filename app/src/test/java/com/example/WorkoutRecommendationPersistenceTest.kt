package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.Program
import com.example.data.model.ProgramDay
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutRecommendationPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `completion persists routine position timestamp and history`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).build()
        try {
            val dao = db.mutantDao()
            val programId = dao.insertProgram(Program(name = "PPL"))
            val pushId = dao.insertProgramDay(ProgramDay(programId = programId, dayIndex = 0, dayCode = "SEG", title = "Push"))
            dao.insertProgramDay(ProgramDay(programId = programId, dayIndex = 1, dayCode = "TER", title = "Pull"))
            val sessionId = dao.insertWorkoutSession(
                WorkoutSession(programDayId = pushId, title = "Push", startedAt = 1_000L)
            )
            val completedAt = 61_000L

            dao.completeWorkoutSession(
                WorkoutSession(
                    id = sessionId,
                    programDayId = pushId,
                    title = "Push",
                    startedAt = 1_000L,
                    finishedAt = completedAt,
                    durationMinutes = 1
                )
            )

            val activeProgram = dao.getActiveProgram().first()
            assertNotNull(activeProgram)
            assertEquals(0, activeProgram?.currentRoutinePosition)
            assertEquals(sessionId, activeProgram?.lastCompletedSessionId)
            assertEquals(completedAt, activeProgram?.lastCompletedAt)
            assertEquals(sessionId, dao.getLastFinishedWorkoutForActiveProgram().first()?.id)
            assertEquals(listOf(sessionId), dao.getFinishedWorkoutSessions().first().map { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `newer ad hoc session does not advance the routine`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).build()
        try {
            val dao = db.mutantDao()
            val programId = dao.insertProgram(Program(name = "PPL"))
            val pushId = dao.insertProgramDay(ProgramDay(programId = programId, dayIndex = 0, dayCode = "SEG", title = "Push"))
            val routineId = dao.insertWorkoutSession(WorkoutSession(programDayId = pushId, title = "Push", finishedAt = 10_000L))
            dao.insertWorkoutSession(WorkoutSession(programDayId = null, title = "Treino avulso", finishedAt = 20_000L))

            assertEquals(routineId, dao.getLastFinishedWorkoutForActiveProgram().first()?.id)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migration 4 to 5 adds durable routine progress fields`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE programs (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                name TEXT NOT NULL,
                                description TEXT NOT NULL,
                                isActive INTEGER NOT NULL,
                                createdAt INTEGER NOT NULL,
                                source TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val database = helper.writableDatabase
            database.execSQL(
                "INSERT INTO programs (name, description, isActive, createdAt, source) VALUES ('PPL', '', 1, 1, 'user')"
            )

            MutantDatabase.MIGRATION_4_5.migrate(database)

            database.query("SELECT currentRoutinePosition, lastCompletedSessionId, lastCompletedAt FROM programs").use { cursor ->
                cursor.moveToFirst()
                assertEquals(-1, cursor.getInt(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
            }
        } finally {
            helper.close()
        }
    }

    @Test
    fun `completion rolls back when progress cannot be persisted`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).build()
        try {
            val dao = db.mutantDao()
            val programId = dao.insertProgram(Program(name = "PPL"))
            val pushId = dao.insertProgramDay(ProgramDay(programId = programId, dayIndex = 0, dayCode = "SEG", title = "Push"))
            val sessionId = dao.insertWorkoutSession(WorkoutSession(programDayId = pushId, title = "Push", startedAt = 1_000L))
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_progress BEFORE UPDATE ON programs BEGIN SELECT RAISE(ABORT, 'Injected failure'); END"
            )

            var failed = false
            try {
                dao.completeWorkoutSession(
                    WorkoutSession(
                        id = sessionId,
                        programDayId = pushId,
                        title = "Push",
                        startedAt = 1_000L,
                        finishedAt = 61_000L
                    )
                )
            } catch (_: Exception) {
                failed = true
            }

            assertEquals(true, failed)
            assertEquals(null, dao.getSessionSync(sessionId)?.finishedAt)
            assertEquals(-1, dao.getActiveProgram().first()?.currentRoutinePosition)
        } finally {
            db.close()
        }
    }
}
