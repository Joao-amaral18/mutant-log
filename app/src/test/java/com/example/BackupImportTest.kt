package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.MutantDao
import com.example.data.db.MutantDatabase
import com.example.data.model.*
import com.example.data.repository.AnalysisExportRepositoryImpl
import com.example.data.repository.BackupImportRepository
import com.example.data.repository.ImportException
import com.example.data.repository.ImportMode
import com.example.data.repository.ImportPreview
import com.example.data.repository.MutantRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupImportTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val json = Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }
    private val databases = mutableListOf<MutantDatabase>()
    private val t0 = 1_780_000_000_000L // mid-2026, well inside the valid range

    @After fun close() = databases.forEach { it.close() }

    private fun newDb(): MutantDatabase = runBlocking {
        Room.inMemoryDatabaseBuilder(context, MutantDatabase::class.java).allowMainThreadQueries().build().also {
            databases += it
            MutantRepository(it.mutantDao()).checkAndSeedInitialData()
        }
    }

    /** Program, custom exercise, two finished workouts with a drop set and cardio, plus an active session. */
    private fun seedHistory(dao: MutantDao) = runBlocking {
        val bundled = dao.getAllExercisesSync().first()
        val custom = dao.insertExercise(Exercise(name = "Cable Y Raise", manufacturer = "Cable", muscleGroup = "Delts",
            seatPosition = "3", notes = "slow negatives", defaultRepMin = 12, defaultRepMax = 15, source = "user"))
        val gym = dao.getAllGymsSync()[1]
        val program = dao.insertProgram(Program(name = "My Split", isActive = true))
        val day = dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 0, dayCode = "MON", title = "Push"))
        dao.insertProgramDay(ProgramDay(programId = program, dayIndex = 1, dayCode = "TUE", title = "Rest", isRestDay = true))
        dao.insertProgramExercise(ProgramExercise(programDayId = day, exerciseId = bundled.id, orderIndex = 0,
            targetWorkSets = 3, repMin = 6, repMax = 8, targetRir = 1, restSeconds = 150))
        dao.insertProgramExercise(ProgramExercise(programDayId = day, exerciseId = custom, orderIndex = 1))

        suspend fun session(start: Long, load: Float, notes: String) {
            val id = dao.insertWorkoutSession(WorkoutSession(programDayId = day, gymId = gym.id, title = "Push",
                startedAt = start, finishedAt = start + 3_600_000, durationMinutes = 60, bodyweight = 82.5f, notes = notes,
                jointDiscomfort = "Mild", readinessScore = 77, sleepScore = 3))
            val main = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = id, exerciseId = bundled.id, orderIndex = 0,
                seatPosition = "#4", executionQuality = "Excellent", targetWorkSets = 3, targetRepMin = 6, targetRepMax = 8))
            dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = main, setNumber = 1, setType = SetType.WARMUP, weightKg = 40f, reps = 10, completedAt = start + 60_000))
            dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = main, setNumber = 2, weightKg = load, reps = 8, rir = 1, completedAt = start + 300_000))
            val drop = dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = main, setNumber = 3, weightKg = load, reps = 7,
                technique = IntensityTechnique.DROP_SET, completedAt = start + 600_000))
            dao.insertSetSegment(SetSegment(workoutSetId = drop, segmentIndex = 0, type = "DROP_SET", weightKg = load - 20f, reps = 6))
            val side = dao.insertWorkoutExercise(WorkoutExercise(workoutSessionId = id, exerciseId = custom, orderIndex = 1, notes = "pump"))
            dao.insertWorkoutSet(WorkoutSet(workoutExerciseId = side, setNumber = 1, weightKg = 12.5f, reps = 14, rir = 2, completedAt = start + 900_000))
            dao.insertCardioSession(CardioSession(workoutSessionId = id, machine = "Stairmaster", durationMinutes = 15, timestamp = start + 3_000_000, notes = "zone 2"))
        }
        session(t0, 80f, "felt strong")
        session(t0 + 3 * 86_400_000L, 85f, "new top set")
        dao.insertCardioSession(CardioSession(machine = "Treadmill", durationMinutes = 30, timestamp = t0 + 86_400_000L))
        dao.rebuildPersonalRecords()
        // An active session is device-local and must not travel in the backup.
        dao.insertWorkoutSession(WorkoutSession(title = "In progress", startedAt = t0 + 5 * 86_400_000L))
    }

    private fun exportJson(dao: MutantDao, legacy: Boolean = false): String = runBlocking {
        val export = AnalysisExportRepositoryImpl(dao).buildExport()
        json.encodeToString(AnalysisExport.serializer(), if (legacy) export.copy(schemaVersion = 1, backup = null) else export)
    }

    private fun preview(db: MutantDatabase, text: String): ImportPreview =
        BackupImportRepository(db).readPreview(text.byteInputStream(), text.length.toLong())

    /** Everything the user would recognise, without database ids. */
    private fun snapshot(dao: MutantDao): List<Any?> = runBlocking {
        val exercises = dao.getEveryExerciseSync().associateBy { it.id }
        val days = dao.getAllProgramDaysSync().associateBy { it.id }
        val gyms = dao.getEveryGymSync().associateBy { it.id }
        val sessions = dao.getAllWorkoutSessionsSync().filter { it.finishedAt != null }
        val wes = dao.getAllWorkoutExercisesSync().groupBy { it.workoutSessionId }
        val sets = dao.getAllWorkoutSetsSync().groupBy { it.workoutExerciseId }
        val segments = dao.getAllSetSegmentsSync().groupBy { it.workoutSetId }
        val sessionTitles = sessions.associate { it.id to "${it.title}@${it.startedAt}" }
        listOf(
            sessions.map { s ->
                listOf(s.title, s.startedAt, s.finishedAt, s.notes, s.bodyweight, s.readinessScore, s.jointDiscomfort, s.sleepScore,
                    days[s.programDayId]?.title, gyms[s.gymId]?.name,
                    wes[s.id].orEmpty().sortedBy { it.orderIndex }.map { we ->
                        listOf(exercises[we.exerciseId]?.name, we.orderIndex, we.seatPosition, we.notes, we.executionQuality, we.targetWorkSets,
                            sets[we.id].orEmpty().sortedBy { it.setNumber }.map { set ->
                                listOf(set.setNumber, set.setType, set.weightKg, set.reps, set.rir, set.technique, set.isPr, set.prType, set.completedAt,
                                    segments[set.id].orEmpty().map { listOf(it.segmentIndex, it.type, it.weightKg, it.reps) })
                            })
                    })
            },
            dao.getAllCardioSessionsSync().map { listOf(it.machine, it.durationMinutes, it.timestamp, it.notes, sessionTitles[it.workoutSessionId]) },
            dao.getAllProgramsSync().map { p ->
                listOf(p.name, p.isActive, dao.getProgramDaysForProgramSync(p.id).map { d ->
                    listOf(d.dayIndex, d.title, d.isRestDay, dao.getAllProgramExercisesSync().filter { it.programDayId == d.id }.map {
                        listOf(exercises[it.exerciseId]?.name, it.targetWorkSets, it.repMin, it.repMax, it.targetRir, it.restSeconds)
                    })
                })
            },
            exercises.values.filter { it.source == "user" }.map { listOf(it.name, it.muscleGroup, it.seatPosition, it.notes, it.defaultRepMin, it.defaultRepMax) },
            dao.getEveryGymSync().map { it.name }
        )
    }

    @Test fun `replace restores a v2 export exactly on a fresh phone`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())

        val target = newDb()
        val preview = preview(target, text)
        assertFalse(preview.legacy)
        assertEquals(2, preview.workouts)
        assertEquals(8, preview.sets)
        assertEquals(3, preview.cardio)

        val result = BackupImportRepository(target).apply(preview, ImportMode.REPLACE)
        assertEquals(2, result.addedWorkouts)
        assertEquals(8, result.addedSets)
        assertEquals(0, result.invalidRows)
        assertEquals(snapshot(source.mutantDao()), snapshot(target.mutantDao()))
        assertNull("the active session stays on the old phone", target.mutantDao().getActiveSessionSync())
        // PR flags travelled: the heavier second session holds a load record.
        assertTrue(target.mutantDao().getAllWorkoutSetsSync().any { it.isPr && it.weightKg == 85f })
    }

    @Test fun `replace removes data logged after the export`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())
        val dao = source.mutantDao()
        dao.deleteActiveWorkoutSession(dao.getActiveSessionSync()!!.id)
        dao.insertWorkoutSession(WorkoutSession(title = "Later", startedAt = t0 + 10 * 86_400_000L, finishedAt = t0 + 10 * 86_400_000L + 1))
        dao.insertProgram(Program(name = "Another", isActive = false))

        BackupImportRepository(source).apply(preview(source, text), ImportMode.REPLACE)
        assertTrue(dao.getAllWorkoutSessionsSync().none { it.title == "Later" })
        assertEquals(listOf("My Split"), dao.getAllProgramsSync().map { it.name })
        assertEquals(2, dao.getAllWorkoutSessionsSync().size)
    }

    @Test fun `merge adds only what is missing and is idempotent`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())
        val target = newDb()
        val repo = BackupImportRepository(target)

        val first = repo.apply(preview(target, text), ImportMode.MERGE)
        assertEquals(2, first.addedWorkouts)
        assertEquals(3, first.addedCardio)
        assertEquals(1, first.addedPrograms)
        assertEquals(1, first.addedExercises)
        val after = snapshot(target.mutantDao())

        val second = repo.apply(preview(target, text), ImportMode.MERGE)
        assertEquals(0, second.addedWorkouts)
        assertEquals(2, second.skippedWorkouts)
        assertEquals(0, second.addedCardio)
        assertEquals(3, second.skippedCardio)
        assertEquals(0, second.addedExercises)
        assertEquals(after, snapshot(target.mutantDao()))
    }

    @Test fun `merge keeps the existing program and links workouts to its days`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())
        val target = newDb()
        val dao = target.mutantDao()
        val mine = dao.insertProgram(Program(name = "my split", isActive = true))
        val myDay = dao.insertProgramDay(ProgramDay(programId = mine, dayIndex = 0, dayCode = "MON", title = "Chest"))

        BackupImportRepository(target).apply(preview(target, text), ImportMode.MERGE)
        assertEquals(listOf("my split"), dao.getAllProgramsSync().map { it.name })
        assertEquals(listOf("Chest"), dao.getProgramDaysForProgramSync(mine).map { it.title })
        assertTrue(dao.getAllWorkoutSessionsSync().all { it.programDayId == myDay })
    }

    @Test fun `an active session blocks both modes and nothing changes`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())
        val target = newDb()
        target.mutantDao().insertWorkoutSession(WorkoutSession(title = "Live"))
        val before = snapshot(target.mutantDao())
        for (mode in ImportMode.values()) {
            try {
                BackupImportRepository(target).apply(preview(target, text), mode)
                fail("import should refuse while a session is active")
            } catch (e: ImportException) {
                assertTrue(e.message!!.contains("current session"))
            }
        }
        assertEquals(before, snapshot(target.mutantDao()))
        assertEquals("Live", target.mutantDao().getActiveSessionSync()?.title)
    }

    @Test fun `a failure halfway through rolls everything back`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao())
        val target = newDb()
        val real = target.mutantDao()
        real.insertWorkoutSession(WorkoutSession(title = "Kept", startedAt = t0 - 86_400_000L, finishedAt = t0 - 86_300_000L))
        val before = snapshot(real)
        // Cardio is written after workouts, sets and programs, so this fails late in the transaction.
        val failing = object : MutantDao by real {
            override suspend fun insertCardioSession(session: CardioSession): Long = error("disk full")
        }
        try {
            BackupImportRepository(target, failing).apply(preview(target, text), ImportMode.REPLACE)
            fail("expected the import to fail")
        } catch (e: IllegalStateException) {
            assertEquals("disk full", e.message)
        }
        assertEquals(before, snapshot(real))
    }

    @Test fun `v1 analysis exports still import with readiness, cardio and records rebuilt`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val text = exportJson(source.mutantDao(), legacy = true)
        val target = newDb()
        val preview = preview(target, text)
        assertTrue(preview.legacy)
        assertTrue(preview.warnings.any { it.startsWith("Older export") })
        assertEquals(2, preview.workouts)

        val result = BackupImportRepository(target).apply(preview, ImportMode.MERGE)
        assertEquals(2, result.addedWorkouts)
        assertEquals(8, result.addedSets)
        assertEquals(3, result.addedCardio)
        assertEquals("only the custom exercise is new; bundled ones match by name", 1, result.addedExercises)
        val dao = target.mutantDao()
        val sessions = dao.getAllWorkoutSessionsSync()
        assertTrue(sessions.all { it.jointDiscomfort == "Mild" && it.sleepScore == 3 && it.bodyweight == 82.5f })
        assertEquals(listOf(15, 30, 15), dao.getAllCardioSessionsSync().map { it.durationMinutes })
        assertEquals(listOf("Stairmaster", "Treadmill", "Stairmaster"), dao.getAllCardioSessionsSync().map { it.machine })
        assertEquals("Other", dao.getEveryExerciseSync().single { it.name == "Cable Y Raise" }.muscleGroup)
        assertEquals(dao.getAllWorkoutSetsSync().count { it.isPr }, source.mutantDao().getAllWorkoutSetsSync().count { it.isPr })
        assertEquals(2, dao.getAllSetSegmentsSync().size)
    }

    @Test fun `files from a newer app or that are not exports are refused`() {
        val db = newDb()
        val repo = BackupImportRepository(db)
        val newer = """{"schemaVersion":3,"generatedAt":"x","app":{},"profile":null,"program":null,"gyms":[],"exerciseVariants":[],"workouts":[],"bodyweight":[],"cardio":[],"readiness":[]}"""
        val message = runCatching { repo.readPreview(newer.byteInputStream()) }.exceptionOrNull()?.message
        assertTrue(message!!.contains("newer version"))
        val junk = runCatching { repo.readPreview("not json at all".byteInputStream()) }.exceptionOrNull()
        assertTrue(junk is ImportException)
        val tooBig = runCatching { repo.readPreview("{}".byteInputStream(), BackupImportRepository.MAX_FILE_BYTES + 1) }.exceptionOrNull()
        assertTrue(tooBig!!.message!!.contains("too large"))
    }

    @Test fun `invalid rows are skipped and counted`() = runBlocking {
        val source = newDb()
        seedHistory(source.mutantDao())
        val export = json.decodeFromString(AnalysisExport.serializer(), exportJson(source.mutantDao()))
        val backup = export.backup!!
        val broken = export.copy(backup = backup.copy(workoutSets = backup.workoutSets.mapIndexed { i, s ->
            when (i) { 0 -> s.copy(reps = 0); 1 -> s.copy(weightKg = -5f); else -> s }
        }))
        val target = newDb()
        val repo = BackupImportRepository(target)
        val result = repo.apply(repo.readPreview(json.encodeToString(AnalysisExport.serializer(), broken).byteInputStream()), ImportMode.REPLACE)
        assertEquals(6, result.addedSets)
        assertEquals(2, result.invalidRows)
    }

    @Test fun `a program-only file becomes the active program on merge`() = runBlocking {
        val target = newDb()
        val dao = target.mutantDao()
        dao.insertProgram(Program(name = "Old Split", isActive = true))
        val bundled = dao.getAllExercisesSync().first()
        val file = AnalysisExport(
            generatedAt = "2026-10-03T00:00:00Z", app = AppExport(), profile = null, program = null,
            gyms = emptyList(), exerciseVariants = emptyList(), workouts = emptyList(), bodyweight = emptyList(),
            cardio = emptyList(), readiness = emptyList(),
            backup = BackupExport(
                databaseVersion = 6,
                exercises = listOf(bundled.copy(id = 900), Exercise(id = 901, name = "Drop Squat Machine", muscleGroup = "Quads", source = "user")),
                programs = listOf(Program(id = 1, name = "New Protocol", isActive = true)),
                programDays = listOf(ProgramDay(id = 10, programId = 1, dayIndex = 0, dayCode = "MON", title = "Legs")),
                programExercises = listOf(
                    ProgramExercise(id = 1, programDayId = 10, exerciseId = 900, orderIndex = 0),
                    ProgramExercise(id = 2, programDayId = 10, exerciseId = 901, orderIndex = 1, targetWorkSets = 2, repMin = 12, repMax = 15)
                )
            )
        )
        val text = json.encodeToString(AnalysisExport.serializer(), file)
        val result = BackupImportRepository(target).apply(preview(target, text), ImportMode.MERGE)
        assertEquals(1, result.addedPrograms)
        assertEquals(1, result.addedExercises)
        assertEquals("New Protocol", dao.getAllProgramsSync().single { it.isActive }.name)
        val day = dao.getAllProgramDaysSync().single { it.title == "Legs" }
        val targets = dao.getAllProgramExercisesSync().filter { it.programDayId == day.id }.sortedBy { it.orderIndex }
        assertEquals(bundled.id, targets[0].exerciseId)
        assertEquals(listOf(12, 15), listOf(targets[1].repMin, targets[1].repMax))
    }

    @Test fun `the Nick Walker protocol file imports and replaces the active program`() = runBlocking {
        val target = newDb()
        val dao = target.mutantDao()
        MutantRepository(dao).adoptNickWalkerTemplate()
        val text = requireNotNull(javaClass.classLoader!!.getResource("nick-walker-protocol.json")).readText()
        val preview = preview(target, text)
        assertFalse(preview.legacy)
        val result = BackupImportRepository(target).apply(preview, ImportMode.MERGE)
        assertEquals(0, result.invalidRows)
        assertEquals("only the exercises missing from the catalog are added", 9, result.addedExercises)
        val active = dao.getAllProgramsSync().single { it.isActive }
        assertEquals("Nick Walker Protocol", active.name)
        val days = dao.getProgramDaysForProgramSync(active.id)
        assertEquals(listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"), days.map { it.dayCode })
        assertEquals(3, days.count { it.isRestDay })
        val targets = dao.getAllProgramExercisesSync().filter { pe -> days.any { it.id == pe.programDayId } }
        assertEquals(28, targets.size)
        assertEquals(11, targets.count { it.programDayId == days.single { d -> d.dayCode == "THU" }.id })
    }
}
