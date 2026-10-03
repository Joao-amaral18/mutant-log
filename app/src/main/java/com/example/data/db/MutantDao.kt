package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

data class ProgramExerciseDetail(
    @Embedded val programExercise: ProgramExercise,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise
)

/** Everything removed with a session exercise, kept so the removal can be undone. */
data class RemovedSessionExercise(
    val sessionId: Long,
    val position: Int,
    val previousExerciseIndex: Int,
    val workoutExercise: WorkoutExercise,
    val sets: List<WorkoutSet>,
    val segments: List<SetSegment>
)

/** A logged set deleted from the active session, kept so it can be put back. */
data class RemovedSet(val set: WorkoutSet, val segments: List<SetSegment>)

/** Per-day plan totals for the protocol list. */
data class ProgramDaySummary(
    val programDayId: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val restSeconds: Int
)

data class WorkoutExerciseDetail(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseId"
    )
    val sets: List<WorkoutSet>
)

data class ExerciseWithVariants(
    @Embedded val exercise: Exercise,
    @Relation(
        parentColumn = "id",
        entityColumn = "exerciseId"
    )
    val variants: List<ExerciseVariant>
)

@Dao
interface MutantDao {

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC, id DESC")
    fun observeWorkoutHistory(): Flow<List<HistoryWorkoutRelation>>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionSync(id: Long): WorkoutSession?
    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSessionSync(): WorkoutSession?
    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutSessionId = :id ORDER BY orderIndex")
    suspend fun getWorkoutDetailsSync(id: Long): List<WorkoutExerciseDetail>
    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)
    @Update
    suspend fun updateWorkoutSet(set: WorkoutSet)

    @Transaction
    suspend fun loadWorkoutDraft(id: Long): WorkoutDraft {
        val session = requireNotNull(getSessionSync(id))
        require(session.finishedAt != null)
        val details = getWorkoutDetailsSync(id)
        val ids = details.flatMap { it.sets }.map { it.id }.toSet()
        return WorkoutDraft(session, details, getAllSetSegmentsSync().filter { it.workoutSetId in ids })
    }
    @Transaction
    suspend fun saveWorkoutDraft(draft: WorkoutDraft) {
        val original = requireNotNull(getSessionSync(draft.session.id))
        require(original.finishedAt != null && draft.session.finishedAt != null)
        check(original.updatedAt == draft.originalUpdatedAt) { "This workout changed. Reopen it before saving." }
        require(draft.session.finishedAt >= draft.session.startedAt)
        val old = getWorkoutDetailsSync(original.id).associateBy { it.workoutExercise.id }
        val seenExercises = mutableSetOf<Long>()
        val seenSets = mutableSetOf<Long>()
        for ((index, detail) in draft.exercises.withIndex()) {
            val we = detail.workoutExercise
            require(we.workoutSessionId == original.id)
            if (we.id > 0) require(we.id in old && seenExercises.add(we.id))
            val normalized = we.copy(orderIndex = index)
            val weId = if (we.id > 0) { updateWorkoutExercise(normalized); we.id } else insertWorkoutExercise(normalized.copy(id = 0))
            val oldSets = old[we.id]?.sets.orEmpty().associateBy { it.id }
            for ((setIndex, set) in detail.sets.withIndex()) {
                require(set.weightKg.isFinite() && set.weightKg >= 0 && set.reps > 0 && set.rir in 0..10)
                if (set.id > 0) require(set.id in oldSets && seenSets.add(set.id))
                val normalizedSet = set.copy(workoutExerciseId = weId, setNumber = setIndex + 1)
                if (set.id > 0) updateWorkoutSet(normalizedSet) else insertWorkoutSet(normalizedSet.copy(id = 0))
            }
            oldSets.keys.filter { it !in seenSets }.forEach { deleteWorkoutSet(it) }
        }
        old.keys.filter { it !in seenExercises }.forEach { deleteWorkoutExercise(it) }
        updateWorkoutSession(draft.session.copy(updatedAt = maxOf(System.currentTimeMillis(), (original.updatedAt ?: 0) + 1)))
        rebuildPersonalRecords()
    }
    /** Correct later records too when a previous performance changes. */
    @Transaction
    suspend fun rebuildPersonalRecords() {
        val sessions = getAllWorkoutSessionsSync().filter { it.finishedAt != null }.associateBy { it.id }
        val exercises = getAllWorkoutExercisesSync().filter { it.workoutSessionId in sessions }.associateBy { it.id }
        val sets = getAllWorkoutSetsSync().filter { it.workoutExerciseId in exercises }
            .sortedWith(compareBy<WorkoutSet> { sessions[exercises[it.workoutExerciseId]!!.workoutSessionId]!!.finishedAt }.thenBy { it.completedAt }.thenBy { it.id })
        val bestLoad = mutableMapOf<Long, Float>()
        val bestReps = mutableMapOf<Pair<Long, Float>, Int>()
        for (set in sets) {
            val exerciseId = exercises[set.workoutExerciseId]!!.exerciseId
            val key = exerciseId to set.weightKg
            val loadPr = set.setType == SetType.WORK && set.weightKg > (bestLoad[exerciseId] ?: 0f)
            val repPr = set.setType == SetType.WORK && bestReps[key] != null && set.reps > bestReps.getValue(key)
            val updated = set.copy(isPr = loadPr || repPr, prType = if (loadPr) "LOAD" else if (repPr) "REP" else "")
            if (updated != set) updateWorkoutSet(updated)
            if (set.setType == SetType.WORK) {
                bestLoad[exerciseId] = maxOf(bestLoad[exerciseId] ?: 0f, set.weightKg)
                bestReps[key] = maxOf(bestReps[key] ?: 0, set.reps)
            }
        }
    }
    @Transaction
    suspend fun createWorkoutSession(session: WorkoutSession, exercises: List<WorkoutExercise>): Long {
        val id = insertWorkoutSession(session)
        exercises.forEach { insertWorkoutExercise(it.copy(workoutSessionId = id)) }
        return id
    }

    @Query("UPDATE workout_sessions SET currentExerciseIndex = :index WHERE id = :id AND finishedAt IS NULL")
    suspend fun selectExercise(id: Long, index: Int)

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :weId ORDER BY setNumber ASC")
    suspend fun getSetsForWorkoutExerciseSync(weId: Long): List<WorkoutSet>
    @Query("SELECT * FROM set_segments WHERE workoutSetId IN (:setIds) ORDER BY workoutSetId ASC, segmentIndex ASC")
    suspend fun getSegmentsForSetsSync(setIds: List<Long>): List<SetSegment>
    @Query("UPDATE workout_exercises SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun setWorkoutExerciseOrder(id: Long, orderIndex: Int)

    /** Today-only addition: the program stays untouched. Inserted at [position] in the session order. */
    @Transaction
    suspend fun addExerciseToActiveSession(sessionId: Long, exerciseId: Long, position: Int): Long {
        val session = checkNotNull(getSessionSync(sessionId)?.takeIf { it.finishedAt == null }) { "Session is no longer active" }
        val current = getWorkoutDetailsSync(session.id).map { it.workoutExercise.id }
        val at = position.coerceIn(0, current.size)
        val id = insertWorkoutExercise(
            WorkoutExercise(workoutSessionId = session.id, exerciseId = exerciseId, orderIndex = at, targetWorkSets = AD_HOC_TARGET_SETS)
        )
        (current.take(at) + id + current.drop(at)).forEachIndexed { index, weId -> setWorkoutExerciseOrder(weId, index) }
        // Keep the user on the exercise they were doing.
        if (at <= session.currentExerciseIndex && current.isNotEmpty()) selectExercise(session.id, session.currentExerciseIndex + 1)
        return id
    }

    /** Removes one exercise and its sets from the active session; the last exercise cannot be removed. */
    @Transaction
    suspend fun removeExerciseFromActiveSession(sessionId: Long, workoutExerciseId: Long): RemovedSessionExercise {
        val session = checkNotNull(getSessionSync(sessionId)?.takeIf { it.finishedAt == null }) { "Session is no longer active" }
        val current = getWorkoutDetailsSync(session.id).map { it.workoutExercise }
        val position = current.indexOfFirst { it.id == workoutExerciseId }
        check(position >= 0) { "Exercise is not in this session" }
        check(current.size > 1) { "Keep at least one exercise" }
        val sets = getSetsForWorkoutExerciseSync(workoutExerciseId)
        val segments = if (sets.isEmpty()) emptyList() else getSegmentsForSetsSync(sets.map { it.id })
        deleteWorkoutExercise(workoutExerciseId)
        val remaining = current.filter { it.id != workoutExerciseId }
        (remaining.map { it.id }).forEachIndexed { index, weId -> setWorkoutExerciseOrder(weId, index) }
        val index = session.currentExerciseIndex
        val nextIndex = when {
            position < index -> index - 1
            else -> index.coerceAtMost(remaining.lastIndex)
        }
        if (nextIndex != index) selectExercise(session.id, nextIndex)
        return RemovedSessionExercise(session.id, position, index, current[position], sets, segments)
    }

    /** Puts a removed exercise back with its original ids, sets and segments. */
    @Transaction
    suspend fun restoreRemovedExercise(removed: RemovedSessionExercise) {
        val session = checkNotNull(getSessionSync(removed.sessionId)?.takeIf { it.finishedAt == null }) { "Session is no longer active" }
        val current = getWorkoutDetailsSync(session.id).map { it.workoutExercise.id }
        insertWorkoutExercise(removed.workoutExercise)
        removed.sets.forEach { insertWorkoutSet(it) }
        removed.segments.forEach { insertSetSegment(it) }
        val at = removed.position.coerceIn(0, current.size)
        (current.take(at) + removed.workoutExercise.id + current.drop(at)).forEachIndexed { index, weId -> setWorkoutExerciseOrder(weId, index) }
        selectExercise(session.id, removed.previousExerciseIndex.coerceIn(0, current.size))
    }
    @Query("UPDATE workout_sessions SET restDeadline = :deadline, restRemainingSeconds = :remaining, restCompleted = :completed, restRecommended = :recommended, restTotalSeconds = :total WHERE id = :id AND finishedAt IS NULL")
    suspend fun setRestState(id: Long, deadline: Long?, remaining: Int, completed: Boolean, recommended: String, total: Int)
    @Query("UPDATE workout_exercises SET nextSetWeightKg = :weight, nextSetReps = :reps WHERE id = :id AND workoutSessionId IN (SELECT id FROM workout_sessions WHERE finishedAt IS NULL)")
    suspend fun updateNextSet(id: Long, weight: Float, reps: Int)
    @Query("UPDATE workout_sessions SET restDeadline = NULL, restRemainingSeconds = 0, restCompleted = 1 WHERE id = :id AND finishedAt IS NULL AND restDeadline = :deadline AND restDeadline <= :now")
    suspend fun completeRestIfDue(id: Long, deadline: Long, now: Long): Int
    /** One commit per logged set: the set, its segments and, when given, the rest timer that follows it. */
    @Transaction
    suspend fun logSet(set: WorkoutSet, segments: List<SetSegment>, restSeconds: Int?, restRecommended: String?): Long {
        val id = insertWorkoutSet(set.copy(setNumber = getSetsForWorkoutExerciseSync(set.workoutExerciseId).size + 1))
        if (segments.isNotEmpty()) insertSetSegments(segments.map { it.copy(id = 0, workoutSetId = id) })
        if (restSeconds != null) changeRestTimer("start", restSeconds, restRecommended)
        return id
    }

    @Transaction
    suspend fun changeRestTimer(action: String, seconds: Int = 0, recommended: String? = null) {
        val session = getActiveSessionSync() ?: return
        val now = System.currentTimeMillis()
        val remaining = session.restSecondsAt(now)
        val running = session.restDeadline != null && remaining > 0
        val next = when (action) { "start" -> seconds.coerceAtLeast(0); "adjust" -> (remaining + seconds).coerceAtLeast(0); "skip", "complete" -> 0; else -> remaining }
        val run = when (action) { "start" -> true; "toggle" -> !running; else -> running }
        val total = when (action) {
            "start" -> next
            "adjust" -> maxOf(session.restTotalSeconds, next)
            "skip" -> 0
            else -> session.restTotalSeconds
        }
        setRestState(session.id, if (run && next > 0) now + next * 1000L else null, next, action == "complete", recommended ?: session.restRecommended, total)
    }

    // --- REFERENCE DATA: Muscle Groups ---
    @Query("SELECT * FROM muscle_groups ORDER BY orderIndex ASC")
    fun getAllMuscleGroups(): Flow<List<MuscleGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscleGroups(groups: List<MuscleGroup>)

    @Query("SELECT COUNT(*) FROM muscle_groups")
    suspend fun countMuscleGroups(): Int

    // --- REFERENCE DATA: Machine Catalog ---
    @Query("SELECT * FROM machine_catalog WHERE isArchived = 0 ORDER BY manufacturer ASC, model ASC")
    fun getAllMachineCatalog(): Flow<List<MachineCatalogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachineCatalog(machines: List<MachineCatalogEntity>)

    @Query("SELECT COUNT(*) FROM machine_catalog WHERE isArchived = 0")
    suspend fun countMachineCatalog(): Int

    // --- REFERENCE DATA: Exercise Variants ---
    @Query("SELECT * FROM exercise_variants WHERE isArchived = 0 ORDER BY variantName ASC")
    fun getAllExerciseVariants(): Flow<List<ExerciseVariant>>

    @Query("SELECT * FROM exercise_variants WHERE exerciseId = :exerciseId AND isArchived = 0 ORDER BY variantName ASC")
    fun getVariantsForExercise(exerciseId: Long): Flow<List<ExerciseVariant>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseVariant(variant: ExerciseVariant)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseVariants(variants: List<ExerciseVariant>)

    @Query("SELECT COUNT(*) FROM exercise_variants WHERE isArchived = 0")
    suspend fun countExerciseVariants(): Int

    // --- REFERENCE DATA: Exercises ---
    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: Long): Exercise?

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup AND isArchived = 0 ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR baseName LIKE '%' || :query || '%' OR muscleGroup LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchExercises(query: String): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>): List<Long>

    @Update
    suspend fun updateExercise(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM exercises WHERE isArchived = 0")
    suspend fun countExercises(): Int

    // --- REFERENCE / USER DATA: Gyms ---
    @Query("SELECT * FROM gyms WHERE isArchived = 0 ORDER BY id ASC")
    fun getAllGyms(): Flow<List<Gym>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGym(gym: Gym): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGyms(gyms: List<Gym>)

    // --- GYM EQUIPMENT ---
    @Query("SELECT * FROM gym_equipment WHERE gymId = :gymId AND isActive = 1")
    fun getEquipmentForGym(gymId: Long): Flow<List<GymEquipmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymEquipment(equipment: GymEquipmentEntity)

    // --- USER DATA: Programs ---
    @Query("SELECT * FROM programs WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun getActiveProgram(): Flow<Program?>

    @Query("SELECT * FROM programs ORDER BY id DESC")
    fun getAllPrograms(): Flow<List<Program>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: Program): Long

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun countPrograms(): Int

    // --- USER DATA: Program Days ---
    @Query("""
        SELECT * FROM program_days
        WHERE programId = (
            SELECT id FROM programs WHERE isActive = 1 ORDER BY id DESC LIMIT 1
        )
        ORDER BY dayIndex ASC
    """)
    fun getAllProgramDays(): Flow<List<ProgramDay>>

    @Query("SELECT * FROM program_days WHERE programId = :programId ORDER BY dayIndex ASC")
    fun getProgramDaysForProgram(programId: Long): Flow<List<ProgramDay>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramDay(day: ProgramDay): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramDays(days: List<ProgramDay>): List<Long>

    @Query("SELECT * FROM program_days WHERE id = :id")
    suspend fun getProgramDayById(id: Long): ProgramDay?

    @Query("SELECT * FROM program_days WHERE programId = :programId ORDER BY dayIndex ASC")
    suspend fun getProgramDaysForProgramSync(programId: Long): List<ProgramDay>

    // --- USER DATA: Program Exercises ---
    @Transaction
    @Query("SELECT * FROM program_exercises WHERE programDayId = :dayId ORDER BY orderIndex ASC")
    fun getProgramExercisesForDay(dayId: Long): Flow<List<ProgramExerciseDetail>>

    @Query("""
        SELECT programDayId, COUNT(*) AS exerciseCount, SUM(targetWorkSets) AS setCount,
               SUM(targetWorkSets * restSeconds) AS restSeconds
        FROM program_exercises GROUP BY programDayId
    """)
    fun observeProgramDaySummaries(): Flow<List<ProgramDaySummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramExercise(pe: ProgramExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgramExercises(list: List<ProgramExercise>)

    // --- USER DATA: Workout Sessions ---
    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun getActiveWorkoutSession(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun getFinishedWorkoutSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    fun getLastFinishedWorkout(): Flow<WorkoutSession?>

    @Query("""
        SELECT ws.* FROM workout_sessions ws
        INNER JOIN program_days pd ON pd.id = ws.programDayId
        WHERE ws.finishedAt IS NOT NULL
          AND pd.programId = (
              SELECT id FROM programs WHERE isActive = 1 ORDER BY id DESC LIMIT 1
          )
        ORDER BY ws.finishedAt DESC, ws.id DESC
        LIMIT 1
    """)
    fun getLastFinishedWorkoutForActiveProgram(): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getWorkoutSessionById(id: Long): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSession): Long

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSession)

    @Query("""
        UPDATE programs
        SET currentRoutinePosition = :position,
            lastCompletedSessionId = :sessionId,
            lastCompletedAt = :completedAt
        WHERE id = :programId
    """)
    suspend fun updateProgramProgress(
        programId: Long,
        position: Int,
        sessionId: Long,
        completedAt: Long
    )

    @Transaction
    suspend fun completeWorkoutSession(session: WorkoutSession) {
        val completedAt = requireNotNull(session.finishedAt)
        updateWorkoutSession(session)

        val completedDay = session.programDayId?.let { getProgramDayById(it) } ?: return
        val trainingDays = getProgramDaysForProgramSync(completedDay.programId)
            .filterNot { it.isRestDay }
            .sortedWith(compareBy<ProgramDay> { it.dayIndex }.thenBy { it.id })
        val position = trainingDays.indexOfFirst { it.id == completedDay.id }
        if (position >= 0) {
            updateProgramProgress(
                programId = completedDay.programId,
                position = position,
                sessionId = session.id,
                completedAt = completedAt
            )
        }
    }

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutSession(id: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :id AND finishedAt IS NULL")
    suspend fun deleteActiveWorkoutSession(id: Long): Int

    @Query("DELETE FROM cardio_sessions WHERE workoutSessionId = :sessionId")
    suspend fun deleteCardioForWorkout(sessionId: Long)

    // SQLite cascades exercises, notes, sets and segments as part of this transaction.
    @Transaction
    suspend fun discardWorkoutSession(sessionId: Long): Boolean {
        if (deleteActiveWorkoutSession(sessionId) == 0) return false
        deleteCardioForWorkout(sessionId)
        return true
    }

    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun countWorkouts(): Int

    // --- USER DATA: Workout Exercises ---
    @Transaction
    @Query("SELECT * FROM workout_exercises WHERE workoutSessionId = :sessionId ORDER BY orderIndex ASC")
    fun getWorkoutExercisesWithDetails(sessionId: Long): Flow<List<WorkoutExerciseDetail>>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExerciseById(id: Long): WorkoutExercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(list: List<WorkoutExercise>): List<Long>

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExercise)

    // --- USER DATA: Sets ---
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :weId ORDER BY setNumber ASC")
    fun getSetsForWorkoutExercise(weId: Long): Flow<List<WorkoutSet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSet(set: WorkoutSet): Long

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteWorkoutSet(setId: Long)

    @Query("SELECT * FROM workout_sets WHERE id = :setId")
    suspend fun getWorkoutSetById(setId: Long): WorkoutSet?

    /** Today-only change to how many work sets an exercise has; only while its session is active. */
    @Query("UPDATE workout_exercises SET targetWorkSets = :sets WHERE id = :id AND workoutSessionId IN (SELECT id FROM workout_sessions WHERE finishedAt IS NULL)")
    suspend fun setSessionTargetSets(id: Long, sets: Int): Int

    /** Deletes a logged set and returns it with its segments so it can be put back. */
    @Transaction
    suspend fun removeSetFromActiveSession(setId: Long): RemovedSet {
        val set = checkNotNull(getWorkoutSetById(setId)) { "Set no longer exists" }
        val segments = getSegmentsForSetsSync(listOf(setId))
        deleteWorkoutSet(setId)
        return RemovedSet(set, segments)
    }

    @Transaction
    suspend fun restoreRemovedSet(removed: RemovedSet) {
        insertWorkoutSet(removed.set)
        removed.segments.forEach { insertSetSegment(it) }
    }

    @Query("SELECT COUNT(*) FROM workout_sets")
    suspend fun countSets(): Int

    // --- History & Progression Queries ---
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workout_sessions s ON we.workoutSessionId = s.id
        WHERE we.exerciseId = :exerciseId AND s.finishedAt IS NOT NULL
        ORDER BY ws.completedAt DESC
    """)
    fun getAllSetsForExercise(exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workout_sessions s ON we.workoutSessionId = s.id
        WHERE we.exerciseId = :exerciseId 
          AND s.finishedAt IS NOT NULL 
          AND s.id = (
             SELECT s2.id FROM workout_sessions s2
             INNER JOIN workout_exercises we2 ON we2.workoutSessionId = s2.id
             WHERE we2.exerciseId = :exerciseId AND s2.finishedAt IS NOT NULL
             ORDER BY s2.finishedAt DESC LIMIT 1
          )
        ORDER BY ws.setNumber ASC
    """)
    suspend fun getLastSessionWorkSetsForExercise(exerciseId: Long): List<WorkoutSet>

    @Query("""
        SELECT we.* FROM workout_exercises we
        INNER JOIN workout_sessions s ON we.workoutSessionId = s.id
        WHERE we.exerciseId = :exerciseId AND s.finishedAt IS NOT NULL
        ORDER BY s.finishedAt DESC LIMIT 1
    """)
    suspend fun getLastWorkoutExercise(exerciseId: Long): WorkoutExercise?

    // --- Set Segments (Rest-Pause, Drop Set) ---
    @Query("SELECT * FROM set_segments WHERE workoutSetId = :setId ORDER BY segmentIndex ASC")
    fun getSegmentsForSet(setId: Long): Flow<List<SetSegment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetSegment(segment: SetSegment): Long

    // --- Cardio ---
    @Query("SELECT * FROM cardio_sessions ORDER BY timestamp DESC")
    fun getAllCardioSessions(): Flow<List<CardioSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioSession(session: CardioSession): Long

    @Query("SELECT COUNT(*) FROM cardio_sessions")
    suspend fun countCardio(): Int

    // --- Volume Analysis Query (Last 7 Days) ---
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workout_sessions s ON we.workoutSessionId = s.id
        WHERE ws.completedAt >= :sinceTimestamp AND ws.setType = 'WORK'
    """)
    fun getWorkSetsSince(sinceTimestamp: Long): Flow<List<WorkoutSet>>

    // --- Export Sync Queries ---
    @Query("SELECT * FROM program_days ORDER BY dayIndex ASC")
    suspend fun getAllProgramDaysSync(): List<ProgramDay>

    @Query("SELECT * FROM program_exercises ORDER BY programDayId ASC, orderIndex ASC")
    suspend fun getAllProgramExercisesSync(): List<ProgramExercise>

    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllExercisesSync(): List<Exercise>

    @Query("SELECT * FROM exercise_variants WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllExerciseVariantsSync(): List<ExerciseVariant>

    @Query("SELECT * FROM machine_catalog WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllMachineCatalogSync(): List<MachineCatalogEntity>

    @Query("SELECT * FROM gyms WHERE isArchived = 0 ORDER BY id ASC")
    suspend fun getAllGymsSync(): List<Gym>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt ASC")
    suspend fun getAllWorkoutSessionsSync(): List<WorkoutSession>

    @Query("SELECT * FROM workout_exercises ORDER BY workoutSessionId ASC, orderIndex ASC")
    suspend fun getAllWorkoutExercisesSync(): List<WorkoutExercise>

    @Query("SELECT * FROM workout_sets ORDER BY workoutExerciseId ASC, setNumber ASC")
    suspend fun getAllWorkoutSetsSync(): List<WorkoutSet>

    @Query("SELECT * FROM set_segments ORDER BY workoutSetId ASC, segmentIndex ASC")
    suspend fun getAllSetSegmentsSync(): List<SetSegment>

    @Query("SELECT * FROM cardio_sessions ORDER BY timestamp ASC")
    suspend fun getAllCardioSessionsSync(): List<CardioSession>

    // --- Backup / restore ---
    // Archived rows are included: finished workouts may still point at them.
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    suspend fun getEveryExerciseSync(): List<Exercise>

    @Query("SELECT * FROM gyms ORDER BY id ASC")
    suspend fun getEveryGymSync(): List<Gym>

    @Query("SELECT * FROM gym_equipment")
    suspend fun getAllGymEquipmentSync(): List<GymEquipmentEntity>

    @Query("SELECT * FROM programs ORDER BY id ASC")
    suspend fun getAllProgramsSync(): List<Program>

    @Query("SELECT * FROM exercise_variants WHERE source = 'user'")
    suspend fun getUserExerciseVariantsSync(): List<ExerciseVariant>

    @Query("SELECT * FROM machine_catalog WHERE source = 'user'")
    suspend fun getUserMachinesSync(): List<MachineCatalogEntity>

    @Query("SELECT id FROM exercise_variants")
    suspend fun getExerciseVariantIdsSync(): List<String>

    @Query("SELECT id FROM machine_catalog")
    suspend fun getMachineIdsSync(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(sets: List<WorkoutSet>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetSegments(segments: List<SetSegment>)

    @Update
    suspend fun updateProgram(program: Program)

    @Query("DELETE FROM cardio_sessions")
    suspend fun deleteAllCardio()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkoutSessions()

    @Query("DELETE FROM program_days")
    suspend fun deleteAllProgramDays()

    @Query("DELETE FROM programs")
    suspend fun deleteAllPrograms()

    @Query("DELETE FROM gyms")
    suspend fun deleteAllGyms()

    @Query("DELETE FROM exercise_variants WHERE source = 'user'")
    suspend fun deleteUserExerciseVariants()

    @Query("DELETE FROM machine_catalog WHERE source = 'user'")
    suspend fun deleteUserMachines()

    @Query("DELETE FROM exercises WHERE source = 'user'")
    suspend fun deleteUserExercises()

    /**
     * Removes everything the user owns before a Replace import. Cascades take workout exercises, sets, segments,
     * program exercises and gym equipment; tables without a foreign key are cleared explicitly.
     * The bundled catalog stays, so bundled exercise ids remain valid.
     */
    @Transaction
    suspend fun clearUserData() {
        deleteAllCardio()
        deleteAllWorkoutSessions()
        deleteAllProgramDays()
        deleteAllPrograms()
        deleteAllGyms()
        deleteUserExerciseVariants()
        deleteUserMachines()
        deleteUserExercises()
    }
}
