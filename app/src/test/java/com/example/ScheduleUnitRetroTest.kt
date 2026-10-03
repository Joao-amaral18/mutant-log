package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import com.example.data.repository.MutantRepository
import com.example.data.repository.ReadinessInput
import com.example.ui.screens.WeekDayStatus
import com.example.ui.screens.buildRotationRows
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ScheduleUnitRetroTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val days = listOf(
        ProgramDay(id = 1, dayIndex = 0, dayCode = "MON", title = "Chest"),
        ProgramDay(id = 2, dayIndex = 1, dayCode = "TUE", title = "Back"),
        ProgramDay(id = 3, dayIndex = 2, dayCode = "WED", title = "Rest", isRestDay = true),
        ProgramDay(id = 4, dayIndex = 3, dayCode = "THU", title = "Shoulders"),
        ProgramDay(id = 5, dayIndex = 4, dayCode = "FRI", title = "Legs")
    )
    private fun done(dayId: Long, at: Long) = WorkoutSession(id = at, programDayId = dayId, title = "", startedAt = at, finishedAt = at + 1)

    @Test fun `rotation rows follow the order and count only the current cycle`() {
        // Chest, Legs and Shoulders were trained out of weekday order; Back is next in the rotation.
        val finished = listOf(done(1, 100), done(5, 200), done(4, 300))
        val rows = buildRotationRows(days, finished, recommendedDayId = 2)
        assertEquals(listOf("#1", "#2", "#3", "#4"), rows.map { it.weekdayCode })
        assertEquals(listOf(WeekDayStatus.DONE, WeekDayStatus.NEXT, WeekDayStatus.DONE, WeekDayStatus.DONE), rows.map { it.status })
        // Once every day is done, the next cycle starts empty.
        val full = buildRotationRows(days, finished + done(2, 400), recommendedDayId = 1)
        assertEquals(listOf(WeekDayStatus.NEXT, WeekDayStatus.PLANNED, WeekDayStatus.PLANNED, WeekDayStatus.PLANNED), full.map { it.status })
    }

    @Test fun `a rotation with no history starts at its first day regardless of weekday`() {
        val friday = java.util.Calendar.getInstance().apply { set(2026, 9, 2, 10, 0) }.timeInMillis
        val rec = WorkoutRecommendationEngine.recommend(days, lastWorkout = null, nowMillis = friday, rotation = true)
        assertEquals(1L, rec.programDayId)
        val afterLegs = WorkoutRecommendationEngine.recommend(days, lastWorkout = done(5, 200), rotation = true)
        assertEquals("rotation wraps to the first day", 1L, afterLegs.programDayId)
    }

    @Test fun `stack-pin progression talks in pins and steps by one`() {
        val sets = listOf(12, 12).mapIndexed { i, r -> WorkoutSet(id = i + 1L, workoutExerciseId = 1, setNumber = i + 1, weightKg = 8f, reps = r) }
        val rec = ProgressionEngine.computeProgression(sets, repMin = 10, repMax = 12, incrementKg = 1f, stack = true)
        assertEquals(ProgressionStatus.INCREASE_LOAD, rec.status)
        assertEquals(9f, rec.suggestedWeightKg)
        assertTrue(rec.previousPerformance.startsWith("pin 8 × 12"))
        assertFalse(rec.reason.contains("kg"))
    }

    @Test fun `stack-pin exercises stay out of kg volume`() {
        val set = HistorySet(WorkoutSet(workoutExerciseId = 1, setNumber = 1, weightKg = 8f, reps = 12))
        val kg = HistoryExercise(WorkoutExercise(workoutSessionId = 1, exerciseId = 1, orderIndex = 0), Exercise(name = "Press", muscleGroup = "Chest"), listOf(set))
        val pin = kg.copy(exercise = kg.exercise.copy(loadUnit = LOAD_UNIT_STACK))
        assertEquals(96.0, kg.volume, 0.0)
        assertEquals(0.0, pin.volume, 0.0)
    }

    @Test fun `a past workout keeps its entered times and spreads set times across them`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val exercise = dao.insertExercise(Exercise(name = "Row", muscleGroup = "Back"))
            val program = dao.insertProgram(Program(name = "Split"))
            val day = dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 0, dayCode = "MON", title = "Back"))
            dao.insertProgramExercise(ProgramExercise(programDayId = day, exerciseId = exercise, orderIndex = 0))
            val repo = MutantRepository(dao)
            val start = 1_780_000_000_000L
            val id = repo.startWorkoutSession(dao.getProgramDayById(day)!!, 1, ReadinessInput(), startedAt = start, retroactiveMinutes = 60)
            val we = dao.getWorkoutDetailsSync(id).single().workoutExercise.id
            // Typed in within a few seconds, long after the workout.
            repeat(3) { repo.logSet(we, SetType.WORK, 50f, 10, 1, IntensityTechnique.NONE) }
            assertTrue("no rest timer for a past workout", dao.getSessionSync(id)!!.restDeadline == null)
            repo.finishWorkout(id)
            val session = dao.getSessionSync(id)!!
            assertTrue(session.isRetroactive)
            assertEquals(start + 60 * 60_000L, session.finishedAt)
            assertEquals(60, session.durationMinutes)
            val times = dao.getWorkoutDetailsSync(id).single().sets.map { it.completedAt }.sorted()
            assertEquals(listOf(start + 15 * 60_000L, start + 30 * 60_000L, start + 45 * 60_000L), times)
        } finally {
            db.close()
        }
    }

    @Test fun `migration 6 to 7 adds unit, schedule and retro columns with safe defaults`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE programs (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO exercises (name) VALUES ('Press')")
            db.execSQL("INSERT INTO programs (name) VALUES ('Split')")
            db.execSQL("INSERT INTO workout_sessions (title) VALUES ('Push')")
            MutantDatabase.MIGRATION_6_7.migrate(db)
            db.query("SELECT loadUnit FROM exercises").use { it.moveToFirst(); assertEquals(LOAD_UNIT_KG, it.getString(0)) }
            db.query("SELECT scheduleMode FROM programs").use { it.moveToFirst(); assertEquals(SCHEDULE_WEEKDAYS, it.getString(0)) }
            db.query("SELECT isRetroactive FROM workout_sessions").use { it.moveToFirst(); assertEquals(0, it.getInt(0)) }
        } finally {
            helper.close()
        }
    }

    @Test fun `check-in joint detail is stored and exported as pre-session`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.mutantDao()
            val program = dao.insertProgram(Program(name = "Split"))
            val day = dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 4, dayCode = "FRI", title = "Legs"))
            val repo = MutantRepository(dao)
            val id = repo.startWorkoutSession(dao.getProgramDayById(day)!!, 1,
                ReadinessInput(jointDiscomfort = "Moderate", jointAreas = setOf("Elbow"), note = "elbow, since biceps day"))
            repo.finishWorkout(id)
            val session = dao.getSessionSync(id)!!
            assertEquals("Elbow", session.jointArea)
            assertEquals("elbow, since biceps day", session.readinessNote)
            val r = com.example.data.repository.AnalysisExportRepositoryImpl(dao).buildExport().readiness.single()
            assertEquals("pre_session", r.measured)
            assertEquals("moderate", r.jointDiscomfortLevel)
            assertEquals(2, r.jointDiscomfort)
            assertEquals(listOf("Elbow"), r.jointAreas)
            assertEquals("elbow, since biceps day", r.note)
            // No discomfort: joints are not kept even if some were tapped earlier.
            val clean = repo.startWorkoutSession(dao.getProgramDayById(day)!!, 1, ReadinessInput(jointAreas = setOf("Knee")))
            assertEquals("", dao.getSessionSync(clean)!!.jointArea)
        } finally {
            db.close()
        }
    }

    @Test fun `migration 7 to 8 adds joint area and note`() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            val db = helper.writableDatabase
            db.execSQL("INSERT INTO workout_sessions (title) VALUES ('Legs')")
            MutantDatabase.MIGRATION_7_8.migrate(db)
            db.query("SELECT jointArea, readinessNote FROM workout_sessions").use {
                it.moveToFirst(); assertEquals("", it.getString(0)); assertEquals("", it.getString(1))
            }
        } finally {
            helper.close()
        }
    }
}
