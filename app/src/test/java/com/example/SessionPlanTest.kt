package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.*
import com.example.data.model.*
import com.example.data.repository.MutantRepository
import com.example.data.repository.ReadinessInput
import com.example.ui.screens.perSetTargets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SessionPlanTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `readiness score stays in range and bands match the check-in`() {
        assertEquals(100, readinessScore(5, 5, 1, "None", 5))
        assertEquals(0, readinessScore(1, 1, 5, "Severe", 1))
        assertEquals(ReadinessBand.READY, readinessBand(ReadinessInput().score))
        assertEquals(ReadinessBand.MODERATE, readinessBand(55))
        assertEquals(ReadinessBand.HIGH_FATIGUE, readinessBand(44))
        // Unknown joint labels count as no pain rather than crashing.
        assertEquals(readinessScore(4, 4, 2, "None", 5), readinessScore(4, 4, 2, "n/a", 5))
    }

    @Test
    fun `per-set targets add a rep per set until the top and reset after a load increase`() {
        val exercise = Exercise(id = 1, name = "Row", muscleGroup = "Back", defaultRepMin = 8, defaultRepMax = 12, defaultWorkSets = 3)
        val detail = WorkoutExerciseDetail(WorkoutExercise(id = 1, workoutSessionId = 1, exerciseId = 1, orderIndex = 0), exercise, emptyList())
        val previous = listOf(11, 10, 12).mapIndexed { i, reps -> WorkoutSet(id = i + 1L, workoutExerciseId = 9, setNumber = i + 1, weightKg = 60f, reps = reps) }
        val maintain = ProgressionRecommendation(60f, 8, 12, 1, ProgressionStatus.MAINTAIN_LOAD, "", "", emptyList())
        assertEquals(listOf(60f to 12, 60f to 11, 60f to 12), perSetTargets(maintain, previous, detail))
        val increase = maintain.copy(suggestedWeightKg = 62.5f, status = ProgressionStatus.INCREASE_LOAD)
        assertEquals(List(3) { 62.5f to 8 }, perSetTargets(increase, previous, detail))
        val first = maintain.copy(status = ProgressionStatus.FIRST_TIME)
        assertTrue(perSetTargets(first, emptyList(), detail).isEmpty())
    }

    @Test
    fun `program day targets are copied into the session and win over exercise defaults`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Press", muscleGroup = "Chest", defaultWorkSets = 2, defaultRestSeconds = 180))
            val program = dao.insertProgram(Program(name = "Split"))
            val dayId = dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 0, dayCode = "MON", title = "Push"))
            dao.insertProgramExercise(ProgramExercise(programDayId = dayId, exerciseId = exercise, orderIndex = 0,
                targetWorkSets = 4, repMin = 6, repMax = 8, targetRir = 1, restSeconds = 150))
            val repository = MutantRepository(dao)
            val session = repository.startWorkoutSession(dao.getProgramDayById(dayId)!!, gymId = 1, ReadinessInput(sleep = 2, energy = 2, soreness = 5))
            val detail = dao.getWorkoutDetailsSync(session).single()
            assertEquals(4, detail.plannedSets)
            assertEquals(6, detail.repMin)
            assertEquals(8, detail.repMax)
            assertEquals(1, detail.plannedRir)
            assertEquals(150, detail.plannedRestSeconds)
            val stored = dao.getSessionSync(session)!!
            assertEquals(ReadinessInput(sleep = 2, energy = 2, soreness = 5).score, stored.readinessScore)
            assertTrue(stored.readinessStatus.contains("fatigue", ignoreCase = true))

            // An ad-hoc addition falls back to the exercise defaults.
            val added = dao.addExerciseToActiveSession(session, exercise, 1)
            val extra = dao.getWorkoutDetailsSync(session).first { it.workoutExercise.id == added }
            assertEquals(2, extra.plannedSets)
            assertEquals(180, extra.plannedRestSeconds)
        } finally {
            db.close()
        }
    }

    @Test
    fun `rest total follows start and adjustments and clears on skip`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val id = dao.insertWorkoutSession(WorkoutSession(title = "Active"))
            dao.changeRestTimer("start", 90)
            assertEquals(90, dao.getSessionSync(id)!!.restTotalSeconds)
            dao.changeRestTimer("adjust", 30)
            assertTrue(dao.getSessionSync(id)!!.restTotalSeconds >= 119)
            dao.changeRestTimer("adjust", -60)
            assertTrue("total never shrinks mid-rest", dao.getSessionSync(id)!!.restTotalSeconds >= 119)
            dao.changeRestTimer("skip")
            assertEquals(0, dao.getSessionSync(id)!!.restTotalSeconds)
        } finally {
            db.close()
        }
    }

    @Test
    fun `migration 5 to 6 adds plan columns and backfills active sessions from the program`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE program_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, programDayId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, variantId TEXT NOT NULL, orderIndex INTEGER NOT NULL, targetWorkSets INTEGER NOT NULL, repMin INTEGER NOT NULL, repMax INTEGER NOT NULL, targetRir INTEGER NOT NULL, restSeconds INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, programDayId INTEGER, title TEXT NOT NULL, finishedAt INTEGER)")
                        db.execSQL("CREATE TABLE workout_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, workoutSessionId INTEGER NOT NULL, exerciseId INTEGER NOT NULL, orderIndex INTEGER NOT NULL)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        try {
            val database = helper.writableDatabase
            database.execSQL("INSERT INTO program_exercises VALUES (1, 7, 3, '', 0, 4, 6, 8, 1, 150)")
            database.execSQL("INSERT INTO workout_sessions VALUES (1, 7, 'Active', NULL)")
            database.execSQL("INSERT INTO workout_sessions VALUES (2, 7, 'Done', 5)")
            database.execSQL("INSERT INTO workout_exercises VALUES (1, 1, 3, 0)")
            database.execSQL("INSERT INTO workout_exercises VALUES (2, 2, 3, 0)")

            MutantDatabase.MIGRATION_5_6.migrate(database)

            database.query("SELECT targetWorkSets, targetRepMin, targetRepMax, targetRir, restSeconds FROM workout_exercises WHERE id = 1").use { c ->
                c.moveToFirst()
                assertEquals(listOf(4, 6, 8, 1, 150), (0..4).map { c.getInt(it) })
            }
            database.query("SELECT targetWorkSets FROM workout_exercises WHERE id = 2").use { c ->
                c.moveToFirst()
                assertTrue("finished sessions keep their history untouched", c.isNull(0))
            }
            database.query("SELECT restTotalSeconds, readinessScore FROM workout_sessions WHERE id = 1").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
                assertTrue(c.isNull(1))
            }
        } finally {
            helper.close()
        }
    }
}
