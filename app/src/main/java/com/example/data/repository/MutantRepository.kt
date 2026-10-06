package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class ReadinessInput(
    val sleep: Int = 4,
    val energy: Int = 4,
    val soreness: Int = 2,
    val jointDiscomfort: String = "None",
    val motivation: Int = 5,
    // Which joints, and a short note such as "elbow, since biceps day". Only kept when there is discomfort.
    val jointAreas: Set<String> = emptySet(),
    val note: String = "",
    // Optional: hours slept and stress 1-5. Recorded for analysis; they do not change the readiness score.
    val sleepHours: Float? = null,
    val stress: Int? = null
) {
    val score: Int get() = readinessScore(sleep, energy, soreness, jointDiscomfort, motivation)
}

data class LibraryCounts(
    val exerciseCount: Int = 124,
    val variantCount: Int = 186,
    val machineCount: Int = 186,
    val muscleGroupCount: Int = 15,
    val programCount: Int = 0,
    val workoutCount: Int = 0,
    val setCount: Int = 0,
    val cardioCount: Int = 0
)

class MutantRepository(private val dao: MutantDao) {

    val workoutHistory: Flow<List<HistoryWorkout>> = dao.observeWorkoutHistory()
        .map { rows -> rows.map { it.toHistory() } }.flowOn(Dispatchers.Default)

    // --- REFERENCE DATA FLOWS ---
    val allExercises: Flow<List<Exercise>> = dao.getAllExercises()
    val allMachineCatalog: Flow<List<MachineCatalogEntity>> = dao.getAllMachineCatalog()
    val allVariants: Flow<List<ExerciseVariant>> = dao.getAllExerciseVariants()
    val allMuscleGroups: Flow<List<MuscleGroup>> = dao.getAllMuscleGroups()
    val allGyms: Flow<List<Gym>> = dao.getAllGyms()

    // --- USER DATA FLOWS ---
    val activeProgram: Flow<Program?> = dao.getActiveProgram()
    val allPrograms: Flow<List<Program>> = dao.getAllPrograms()
    val allProgramDays: Flow<List<ProgramDay>> = dao.getAllProgramDays()
    val activeWorkoutSession: Flow<WorkoutSession?> = dao.getActiveWorkoutSession()
    val finishedWorkouts: Flow<List<WorkoutSession>> = dao.getFinishedWorkoutSessions()
    val lastFinishedWorkout: Flow<WorkoutSession?> = dao.getLastFinishedWorkout()
    val lastFinishedRoutineWorkout: Flow<WorkoutSession?> = dao.getLastFinishedWorkoutForActiveProgram()
    val allCardioSessions: Flow<List<CardioSession>> = dao.getAllCardioSessions()

    fun getVariantsForExercise(exerciseId: Long): Flow<List<ExerciseVariant>> =
        dao.getVariantsForExercise(exerciseId)

    fun searchExercises(query: String): Flow<List<Exercise>> =
        dao.searchExercises(query)

    fun getProgramExercisesForDay(dayId: Long): Flow<List<ProgramExerciseDetail>> =
        dao.getProgramExercisesForDay(dayId)

    val programDaySummaries: Flow<List<ProgramDaySummary>> = dao.observeProgramDaySummaries()

    suspend fun getPreviousWorkSets(exerciseId: Long): List<WorkoutSet> = withContext(Dispatchers.IO) {
        dao.getLastSessionWorkSetsForExercise(exerciseId).filter { it.setType == SetType.WORK }
    }

    fun getWorkoutExercisesWithDetails(sessionId: Long): Flow<List<WorkoutExerciseDetail>> =
        dao.getWorkoutExercisesWithDetails(sessionId)

    fun getExerciseHistory(exerciseId: Long): Flow<List<WorkoutSet>> =
        dao.getAllSetsForExercise(exerciseId)

    suspend fun getExerciseById(id: Long): Exercise? = dao.getExerciseById(id)

    suspend fun getProgressionSuggestion(
        exerciseId: Long,
        repMin: Int = 10,
        repMax: Int = 12,
        targetRir: Int = 0,
        incrementKg: Float = 2.5f,
        stack: Boolean = false
    ): ProgressionRecommendation = withContext(Dispatchers.IO) {
        val lastSets = dao.getLastSessionWorkSetsForExercise(exerciseId)
        val lastWe = dao.getLastWorkoutExercise(exerciseId)
        ProgressionEngine.computeProgression(
            lastWorkSets = lastSets,
            repMin = repMin,
            repMax = repMax,
            targetRir = targetRir,
            incrementKg = incrementKg,
            lastExecutionQuality = lastWe?.executionQuality ?: "Good",
            lastTargetMuscleQuality = lastWe?.targetMuscleQuality ?: "Good",
            stack = stack
        )
    }

    // --- SEEDING CANONICAL REFERENCE DATA ONLY ---
    // Pure reference data: 124 exercises, 186 machines, 15 muscle groups, 186 variants.
    // NEVER seed workouts, sets, cardio, bodyweight, or fabricated progress!
    suspend fun checkAndSeedInitialData() = withContext(Dispatchers.IO) {
        val count = dao.countExercises()
        if (count == 0) {
            dao.insertMuscleGroups(ReferenceCatalogData.canonicalMuscleGroups)
            dao.insertMachineCatalog(ReferenceCatalogData.canonicalMachineCatalog)
            dao.insertExercises(ReferenceCatalogData.canonicalExercises)
            dao.insertExerciseVariants(ReferenceCatalogData.canonicalExerciseVariants)
            dao.insertGyms(ReferenceCatalogData.defaultGyms)
            // Fresh install state: 0 programs, 0 workouts, 0 sets, 0 cardio!
        }
    }

    suspend fun getLibraryCounts(): LibraryCounts = withContext(Dispatchers.IO) {
        LibraryCounts(
            exerciseCount = dao.countExercises(),
            variantCount = dao.countExerciseVariants(),
            machineCount = dao.countMachineCatalog(),
            muscleGroupCount = dao.countMuscleGroups(),
            programCount = dao.countPrograms(),
            workoutCount = dao.countWorkouts(),
            setCount = dao.countSets(),
            cardioCount = dao.countCardio()
        )
    }

    // --- USER PROGRAM ACTIONS ---
    suspend fun adoptNickWalkerTemplate(): Long = withContext(Dispatchers.IO) {
        val (templateProgram, daysAndExercises) = ReferenceCatalogData.createNickWalkerTemplate()
        val programId = dao.insertProgram(templateProgram)

        daysAndExercises.forEach { (day, exercises) ->
            val dayId = dao.insertProgramDay(day.copy(programId = programId))
            val peList = exercises.map { pe -> pe.copy(programDayId = dayId) }
            dao.insertProgramExercises(peList)
        }
        programId
    }

    suspend fun createProgram(name: String, description: String = "", days: List<ProgramDay>): Long = withContext(Dispatchers.IO) {
        val program = Program(name = name, description = description, isActive = true, source = "user")
        val programId = dao.insertProgram(program)
        days.forEach { day ->
            dao.insertProgramDay(day.copy(programId = programId))
        }
        programId
    }

    suspend fun addProgramExercise(
        programDayId: Long,
        exerciseId: Long,
        variantId: String = "",
        targetSets: Int = 2,
        repMin: Int = 10,
        repMax: Int = 12,
        targetRir: Int = 0,
        restSeconds: Int = 180
    ): Long = withContext(Dispatchers.IO) {
        val existing = dao.getProgramExercisesForDay(programDayId).firstOrNull() ?: emptyList()
        val nextOrder = existing.size + 1
        dao.insertProgramExercise(
            ProgramExercise(
                programDayId = programDayId,
                exerciseId = exerciseId,
                variantId = variantId,
                orderIndex = nextOrder,
                targetWorkSets = targetSets,
                repMin = repMin,
                repMax = repMax,
                targetRir = targetRir,
                restSeconds = restSeconds
            )
        )
    }

    suspend fun createCustomVariant(
        exerciseId: Long,
        exerciseStableId: String,
        variantName: String,
        manufacturer: String,
        resistanceType: String,
        incrementKg: Float = 2.5f,
        seat: String = "",
        handle: String = "",
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        val id = "user_var_${System.currentTimeMillis()}"
        dao.insertExerciseVariant(
            ExerciseVariant(
                id = id,
                exerciseId = exerciseId,
                exerciseStableId = exerciseStableId,
                variantName = variantName,
                manufacturer = manufacturer,
                resistanceType = resistanceType,
                weightIncrementKg = incrementKg,
                defaultSeat = seat,
                defaultHandle = handle,
                notes = notes,
                source = "user"
            )
        )
    }

    // --- WORKOUT EXECUTION ACTIONS ---
    /** With [retroactiveMinutes], the session is a workout done earlier: it starts at [startedAt] and lasts that long. */
    suspend fun startWorkoutSession(
        programDay: ProgramDay,
        gymId: Long,
        readiness: ReadinessInput,
        startedAt: Long = System.currentTimeMillis(),
        retroactiveMinutes: Int? = null
    ): Long = withContext(Dispatchers.IO) {
        val calculatedStatus = calculateReadinessStatus(readiness)
        val session = WorkoutSession(
            programDayId = programDay.id,
            gymId = gymId,
            title = programDay.title,
            startedAt = startedAt,
            finishedAt = null,
            durationMinutes = retroactiveMinutes ?: 0,
            isRetroactive = retroactiveMinutes != null,
            sleepScore = readiness.sleep,
            energyScore = readiness.energy,
            sorenessScore = readiness.soreness,
            jointDiscomfort = readiness.jointDiscomfort,
            jointArea = if (readiness.jointDiscomfort.equals("None", ignoreCase = true)) "" else readiness.jointAreas.joinToString(", "),
            readinessNote = readiness.note.trim().take(200),
            motivationScore = readiness.motivation,
            sleepHours = readiness.sleepHours,
            stressScore = readiness.stress,
            readinessStatus = calculatedStatus,
            readinessScore = readiness.score
        )

        // Pre-populate workout exercises from program day
        val programExercises = dao.getProgramExercisesForDay(programDay.id).firstOrNull() ?: emptyList()
        val exercises = programExercises.map { peDetail ->
            val we = WorkoutExercise(
                workoutSessionId = 0,
                exerciseId = peDetail.exercise.id,
                variantId = peDetail.programExercise.variantId,
                orderIndex = peDetail.programExercise.orderIndex,
                targetWorkSets = peDetail.programExercise.targetWorkSets.coerceAtLeast(1),
                targetRepMin = peDetail.programExercise.repMin,
                targetRepMax = peDetail.programExercise.repMax,
                targetRir = peDetail.programExercise.targetRir,
                restSeconds = peDetail.programExercise.restSeconds,
                seatPosition = peDetail.exercise.seatPosition,
                handlePosition = peDetail.exercise.handlePosition,
                notes = peDetail.exercise.notes,
                executionQuality = "Good",
                targetMuscleQuality = "Good"
            )
            we
        }
        dao.createWorkoutSession(session, exercises)
    }

    suspend fun startAdHocWorkoutSession(
        title: String,
        gymId: Long,
        exerciseIds: List<Long>,
        readiness: ReadinessInput
    ): Long = withContext(Dispatchers.IO) {
        val calculatedStatus = calculateReadinessStatus(readiness)
        val session = WorkoutSession(
            programDayId = null,
            gymId = gymId,
            title = title,
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            sleepScore = readiness.sleep,
            energyScore = readiness.energy,
            sorenessScore = readiness.soreness,
            jointDiscomfort = readiness.jointDiscomfort,
            jointArea = if (readiness.jointDiscomfort.equals("None", ignoreCase = true)) "" else readiness.jointAreas.joinToString(", "),
            readinessNote = readiness.note.trim().take(200),
            motivationScore = readiness.motivation,
            sleepHours = readiness.sleepHours,
            stressScore = readiness.stress,
            readinessStatus = calculatedStatus,
            readinessScore = readiness.score
        )

        val exercises = exerciseIds.mapIndexed { index, exId ->
            val ex = dao.getExerciseById(exId)
            val we = WorkoutExercise(
                workoutSessionId = 0,
                exerciseId = exId,
                orderIndex = index + 1,
                seatPosition = ex?.seatPosition ?: "",
                handlePosition = ex?.handlePosition ?: "",
                notes = ex?.notes ?: "",
                executionQuality = "Good",
                targetMuscleQuality = "Good"
            )
            we
        }
        dao.createWorkoutSession(session, exercises)
    }

    private fun calculateReadinessStatus(r: ReadinessInput): String {
        val isJointHigh = r.jointDiscomfort.equals("Severe", ignoreCase = true) ||
                r.jointDiscomfort.equals("Moderate", ignoreCase = true)
        val isEnergyLow = r.energy <= 2 || r.sleep <= 2
        val isSorenessHigh = r.soreness >= 4

        val lowScore = readinessBand(r.score) == ReadinessBand.HIGH_FATIGUE
        return if (isJointHigh || (isEnergyLow && isSorenessHigh) || lowScore) {
            "Accumulated fatigue detected"
        } else {
            "Normal"
        }
    }

    suspend fun discardWorkout(sessionId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.discardWorkoutSession(sessionId)
    }

    suspend fun finishWorkout(sessionId: Long, notes: String = "", bodyweight: Float = 0f) = withContext(Dispatchers.IO) {
        val session = dao.getWorkoutSessionById(sessionId).firstOrNull() ?: return@withContext
        // A past workout ends when the user said it did, not when they finished typing it in.
        val durationMins = if (session.isRetroactive) session.durationMinutes.coerceAtLeast(1)
            else ((System.currentTimeMillis() - session.startedAt) / 60000).toInt().coerceAtLeast(1)
        val finishTime = if (session.isRetroactive) session.startedAt + durationMins * 60_000L else System.currentTimeMillis()
        val updated = session.copy(
            finishedAt = finishTime,
            durationMinutes = durationMins,
            notes = notes,
            bodyweight = if (bodyweight > 0) bodyweight else session.bodyweight
        )
        dao.completeWorkoutSession(updated.copy(restDeadline = null, restRemainingSeconds = 0))
        if (session.isRetroactive) dao.spreadSetTimes(session.id, session.startedAt, finishTime)
        dao.rebuildPersonalRecords()
    }

    /** Logs one set with its segments; with [restSeconds] the rest timer starts in the same transaction. */
    suspend fun logSet(
        workoutExerciseId: Long,
        setType: SetType,
        weightKg: Float,
        reps: Int,
        rir: Int,
        technique: IntensityTechnique,
        segments: List<SetSegment> = emptyList(),
        restSeconds: Int? = null,
        restRecommended: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val newSet = WorkoutSet(
            workoutExerciseId = workoutExerciseId,
            setNumber = 0, // numbered inside the transaction
            setType = setType,
            weightKg = weightKg,
            reps = reps,
            rir = rir,
            technique = technique,
            completedAt = System.currentTimeMillis()
        )
        dao.logSet(newSet, segments, restSeconds, restRecommended)
    }

    suspend fun addExerciseToSession(sessionId: Long, exerciseId: Long, position: Int): Long = withContext(Dispatchers.IO) {
        dao.addExerciseToActiveSession(sessionId, exerciseId, position)
    }

    suspend fun removeExerciseFromSession(sessionId: Long, workoutExerciseId: Long): RemovedSessionExercise =
        withContext(Dispatchers.IO) { dao.removeExerciseFromActiveSession(sessionId, workoutExerciseId) }

    suspend fun restoreSessionExercise(removed: RemovedSessionExercise) = withContext(Dispatchers.IO) {
        dao.restoreRemovedExercise(removed)
    }

    suspend fun setSessionTargetSets(workoutExerciseId: Long, sets: Int) = withContext(Dispatchers.IO) {
        check(dao.setSessionTargetSets(workoutExerciseId, sets) == 1) { "Session is no longer active" }
    }

    suspend fun removeLoggedSet(setId: Long): RemovedSet = withContext(Dispatchers.IO) { dao.removeSetFromActiveSession(setId) }

    suspend fun restoreLoggedSet(removed: RemovedSet) = withContext(Dispatchers.IO) { dao.restoreRemovedSet(removed) }

    suspend fun deleteSet(setId: Long) = withContext(Dispatchers.IO) {
        dao.deleteWorkoutSet(setId)
    }

    suspend fun updateWorkoutExercise(
        workoutExerciseId: Long,
        executionQuality: String,
        targetMuscleQuality: String,
        seatPosition: String,
        handlePosition: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val current = dao.getWorkoutExerciseById(workoutExerciseId) ?: return@withContext
        val updated = current.copy(
            executionQuality = executionQuality,
            targetMuscleQuality = targetMuscleQuality,
            seatPosition = seatPosition,
            handlePosition = handlePosition,
            notes = notes
        )
        dao.updateWorkoutExercise(updated)
    }

    suspend fun logCardio(session: CardioSession): Long = withContext(Dispatchers.IO) {
        dao.insertCardioSession(session)
    }
}
