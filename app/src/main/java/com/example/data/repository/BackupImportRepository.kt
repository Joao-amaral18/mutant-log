package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.MutantDao
import com.example.data.db.MutantDatabase
import com.example.data.db.ReferenceCatalogData
import com.example.data.model.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

enum class ImportMode { MERGE, REPLACE }

/** A parsed file waiting for the user to pick a mode. */
data class ImportPreview(
    val backup: BackupExport,
    val legacy: Boolean,
    val generatedAt: String,
    val workouts: Int,
    val sets: Int,
    val cardio: Int,
    val firstWorkoutAt: Long?,
    val lastWorkoutAt: Long?,
    val warnings: List<String>
)

data class ImportResult(
    val addedWorkouts: Int,
    val skippedWorkouts: Int,
    val addedSets: Int,
    val addedCardio: Int,
    val skippedCardio: Int,
    val addedExercises: Int,
    val addedPrograms: Int,
    val invalidRows: Int
)

/** A problem the user can act on; the message is shown as is. */
class ImportException(message: String) : Exception(message)

/**
 * Restores a Mutant Log JSON export. Everything happens in one transaction, so a failure leaves the
 * database untouched, and file ids are always remapped, never reused.
 */
class BackupImportRepository(
    private val database: MutantDatabase,
    private val dao: MutantDao = database.mutantDao()
) {
    @OptIn(ExperimentalSerializationApi::class)
    fun readPreview(input: InputStream, sizeBytes: Long? = null): ImportPreview {
        if (sizeBytes != null && sizeBytes > MAX_FILE_BYTES) throw ImportException(TOO_LARGE)
        val export = try {
            LimitedInputStream(input, MAX_FILE_BYTES).use { json.decodeFromStream(AnalysisExport.serializer(), it) }
        } catch (e: FileTooLarge) {
            throw ImportException(TOO_LARGE)
        } catch (e: SerializationException) {
            throw ImportException(NOT_AN_EXPORT)
        } catch (e: IllegalArgumentException) {
            throw ImportException(NOT_AN_EXPORT)
        }
        if (export.schemaVersion > EXPORT_SCHEMA_VERSION) {
            throw ImportException("This file was made by a newer version of Mutant Log. Update the app to import it.")
        }
        val backup = export.backup ?: legacyToBackup(export, export.app.databaseVersion)
        val finished = backup.workoutSessions.filter { it.finishedAt != null }
        val finishedIds = finished.mapTo(HashSet()) { it.id }
        val exerciseIds = backup.workoutExercises.filter { it.workoutSessionId in finishedIds }.mapTo(HashSet()) { it.id }
        val warnings = buildList {
            if (!export.app.name.equals("Mutant Log", ignoreCase = true)) add("The file doesn't say it came from Mutant Log.")
            if (export.backup == null) add("Older export: program, exercise defaults and notes may be incomplete.")
        }
        return ImportPreview(
            backup = backup,
            legacy = export.backup == null,
            generatedAt = export.generatedAt,
            workouts = finished.size,
            sets = backup.workoutSets.count { it.workoutExerciseId in exerciseIds },
            cardio = backup.cardioSessions.size,
            firstWorkoutAt = finished.minOfOrNull { it.startedAt },
            lastWorkoutAt = finished.maxOfOrNull { it.startedAt },
            warnings = warnings
        )
    }

    suspend fun apply(
        preview: ImportPreview,
        mode: ImportMode,
        now: Long = System.currentTimeMillis()
    ): ImportResult = database.withTransaction {
        // Checked inside the transaction so a session cannot start in between.
        if (dao.getActiveSessionSync() != null) throw ImportException("Finish or discard the current session before importing.")
        val backup = preview.backup
        val merge = mode == ImportMode.MERGE
        if (!merge) dao.clearUserData()
        var invalid = 0
        fun validTime(t: Long) = t in EARLIEST..(now + DAY_MS)

        // Exercises: match by stable id, then by name and machine. Bundled rows keep their ids.
        val existing = dao.getEveryExerciseSync()
        val byStable = existing.filter { it.stableId.isNotBlank() }.associateBy { it.stableId }
        val byKey = existing.groupBy { exerciseKey(it) }.mapValues { it.value.first() }
        val exerciseIds = HashMap<Long, Long>()
        var addedExercises = 0
        backup.exercises.forEach { ex ->
            if (ex.name.isBlank()) { invalid++; return@forEach }
            val match = ex.stableId.takeIf { it.isNotBlank() }?.let(byStable::get) ?: byKey[exerciseKey(ex)]
            when {
                match == null -> {
                    exerciseIds[ex.id] = dao.insertExercise(ex.copy(id = 0))
                    addedExercises++
                }
                merge -> exerciseIds[ex.id] = match.id
                else -> {
                    // @Update, never insert-with-replace: a replaced parent would cascade-delete its children.
                    dao.updateExercise(
                        if (preview.legacy) match.copy(
                            seatPosition = ex.seatPosition.ifBlank { match.seatPosition },
                            handlePosition = ex.handlePosition.ifBlank { match.handlePosition },
                            notes = ex.notes.ifBlank { match.notes }
                        ) else ex.copy(id = match.id, stableId = ex.stableId.ifBlank { match.stableId })
                    )
                    exerciseIds[ex.id] = match.id
                }
            }
        }

        val machineIds = dao.getMachineIdsSync().toHashSet()
        backup.machineCatalog.filter { it.id !in machineIds }.takeIf { it.isNotEmpty() }?.let {
            dao.insertMachineCatalog(it)
            machineIds += it.map { m -> m.id }
        }
        val variantIds = dao.getExerciseVariantIdsSync().toHashSet()
        backup.exerciseVariants.forEach { v ->
            val exerciseId = exerciseIds[v.exerciseId] ?: run { invalid++; return@forEach }
            if (v.id !in variantIds) dao.insertExerciseVariant(v.copy(exerciseId = exerciseId))
        }

        // Gyms by name.
        val gymsByName = dao.getEveryGymSync().associateBy { it.name.trim().lowercase() }
        val gymIds = HashMap<Long, Long>()
        backup.gyms.forEach { g ->
            gymIds[g.id] = gymsByName[g.name.trim().lowercase()]?.id ?: dao.insertGym(g.copy(id = 0))
        }
        backup.gymEquipment.forEach { e ->
            val gymId = gymIds[e.gymId] ?: return@forEach
            dao.insertGymEquipment(e.copy(gymId = gymId, machineCatalogId = e.machineCatalogId?.takeIf { it in machineIds }))
        }

        // Programs. Merge keeps an existing program with the same name untouched and links days by index.
        val existingPrograms = if (merge) dao.getAllProgramsSync() else emptyList()
        val hasActiveProgram = existingPrograms.any { it.isActive }
        val programIds = HashMap<Long, Long>()
        val dayIds = HashMap<Long, Long>()
        val insertedPrograms = mutableListOf<Pair<Program, Long>>()
        var addedPrograms = 0
        backup.programs.forEach { p ->
            val days = backup.programDays.filter { it.programId == p.id }
            val match = existingPrograms.firstOrNull { it.name.equals(p.name, ignoreCase = true) }
            if (match != null) {
                programIds[p.id] = match.id
                val local = dao.getProgramDaysForProgramSync(match.id).associateBy { it.dayIndex }
                days.forEach { d -> local[d.dayIndex]?.let { dayIds[d.id] = it.id } }
                return@forEach
            }
            val programId = dao.insertProgram(
                p.copy(id = 0, isActive = p.isActive && !hasActiveProgram, lastCompletedSessionId = null)
            )
            programIds[p.id] = programId
            insertedPrograms += p to programId
            addedPrograms++
            days.forEach { d -> dayIds[d.id] = dao.insertProgramDay(d.copy(id = 0, programId = programId)) }
            val targets = backup.programExercises.mapNotNull { pe ->
                val dayId = dayIds[pe.programDayId] ?: return@mapNotNull null
                if (days.none { it.id == pe.programDayId }) return@mapNotNull null
                val exerciseId = exerciseIds[pe.exerciseId] ?: run { invalid++; return@mapNotNull null }
                pe.copy(id = 0, programDayId = dayId, exerciseId = exerciseId)
            }
            if (targets.isNotEmpty()) dao.insertProgramExercises(targets)
        }

        // Workouts. Merge treats the same start time and title as the same workout.
        val existingSessions = if (merge) dao.getAllWorkoutSessionsSync().associateBy { it.startedAt to it.title } else emptyMap()
        val exercisesBySession = backup.workoutExercises.groupBy { it.workoutSessionId }
        val setsByExercise = backup.workoutSets.groupBy { it.workoutExerciseId }
        val segmentsBySet = backup.setSegments.groupBy { it.workoutSetId }
        val sessionIds = HashMap<Long, Long>()
        var addedWorkouts = 0
        var skippedWorkouts = 0
        var addedSets = 0
        backup.workoutSessions.sortedBy { it.startedAt }.forEach { s ->
            val finishedAt = s.finishedAt ?: return@forEach
            if (!validTime(s.startedAt) || finishedAt < s.startedAt || !validTime(finishedAt)) { invalid++; return@forEach }
            existingSessions[s.startedAt to s.title]?.let {
                sessionIds[s.id] = it.id
                skippedWorkouts++
                return@forEach
            }
            val sessionId = dao.insertWorkoutSession(
                s.copy(
                    id = 0,
                    programDayId = s.programDayId?.let(dayIds::get),
                    gymId = s.gymId?.let(gymIds::get),
                    currentExerciseIndex = 0,
                    restDeadline = null, restRemainingSeconds = 0, restCompleted = false, restTotalSeconds = 0
                )
            )
            sessionIds[s.id] = sessionId
            addedWorkouts++
            exercisesBySession[s.id].orEmpty().sortedBy { it.orderIndex }.forEach { we ->
                val exerciseId = exerciseIds[we.exerciseId] ?: run { invalid++; return@forEach }
                val workoutExerciseId = dao.insertWorkoutExercise(we.copy(id = 0, workoutSessionId = sessionId, exerciseId = exerciseId))
                val (valid, bad) = setsByExercise[we.id].orEmpty().partition { validSet(it) }
                invalid += bad.size
                if (valid.isEmpty()) return@forEach
                val newIds = dao.insertWorkoutSets(valid.map { set ->
                    set.copy(id = 0, workoutExerciseId = workoutExerciseId,
                        completedAt = set.completedAt.takeIf(::validTime) ?: s.startedAt)
                })
                addedSets += newIds.size
                val segments = valid.zip(newIds).flatMap { (set, newId) ->
                    segmentsBySet[set.id].orEmpty().filter { validSegment(it).also { ok -> if (!ok) invalid++ } }
                        .map { it.copy(id = 0, workoutSetId = newId) }
                }
                if (segments.isNotEmpty()) dao.insertSetSegments(segments)
            }
        }

        // Routine position refers to a session, so it is restored once sessions have new ids.
        insertedPrograms.forEach { (original, programId) ->
            val last = original.lastCompletedSessionId?.let(sessionIds::get) ?: return@forEach
            dao.updateProgramProgress(programId, original.currentRoutinePosition, last, original.lastCompletedAt ?: now)
        }

        val existingCardio = if (merge) dao.getAllCardioSessionsSync().mapTo(HashSet()) { it.timestamp to it.machine.lowercase() } else emptySet()
        var addedCardio = 0
        var skippedCardio = 0
        backup.cardioSessions.forEach { c ->
            if (!validTime(c.timestamp) || c.durationMinutes !in 0..MAX_CARDIO_MINUTES) { invalid++; return@forEach }
            if ((c.timestamp to c.machine.lowercase()) in existingCardio) { skippedCardio++; return@forEach }
            dao.insertCardioSession(c.copy(id = 0, workoutSessionId = c.workoutSessionId?.let(sessionIds::get)))
            addedCardio++
        }

        // A Replace from a file without gyms must still leave the app usable.
        if (!merge && dao.getEveryGymSync().isEmpty()) dao.insertGyms(ReferenceCatalogData.defaultGyms)
        dao.rebuildPersonalRecords()

        ImportResult(addedWorkouts, skippedWorkouts, addedSets, addedCardio, skippedCardio, addedExercises, addedPrograms, invalid)
    }

    private fun exerciseKey(e: Exercise) = "${e.name.trim().lowercase()}|${e.manufacturer.trim().lowercase()}"

    private fun validSet(s: WorkoutSet) =
        s.weightKg.isFinite() && s.weightKg in 0f..MAX_LOAD_KG && s.reps in 1..999 && s.rir in 0..10

    private fun validSegment(s: SetSegment) = s.weightKg.isFinite() && s.weightKg in 0f..MAX_LOAD_KG && s.reps in 1..999

    private class FileTooLarge : IOException()

    /** Stops reading once [limit] bytes have passed, so a huge file cannot exhaust memory. */
    private class LimitedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {
        private var count = 0L
        private fun track(n: Int): Int {
            if (n > 0) {
                count += n
                if (count > limit) throw FileTooLarge()
            }
            return n
        }
        override fun read(): Int = super.read().also { if (it >= 0) track(1) }
        override fun read(b: ByteArray, off: Int, len: Int): Int = track(super.read(b, off, len))
    }

    companion object {
        const val MAX_FILE_BYTES = 50L * 1024 * 1024
        private const val MAX_LOAD_KG = 1000f
        private const val MAX_CARDIO_MINUTES = 24 * 60
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val EARLIEST = 946_684_800_000L // 2000-01-01
        private const val TOO_LARGE = "This file is too large to be a Mutant Log export."
        private const val NOT_AN_EXPORT = "This isn't a Mutant Log export, or the file is damaged."
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
