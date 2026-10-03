package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Rebuilds database rows from a v1 (analysis-only) export so older files can still be imported.
 * v1 drops some fields, so these come back as defaults:
 * - exercise muscle group, rep ranges and cues;
 * - session notes;
 * - personal-record flags, which Import rebuilds.
 */
fun legacyToBackup(export: AnalysisExport, databaseVersion: Int): BackupExport {
    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    fun time(value: String?): Long? = value?.let { runCatching { iso.parse(it)?.time }.getOrNull() }

    val exercises = linkedMapOf<Long, Exercise>()
    export.exerciseVariants.forEach { v ->
        val id = legacyId(v.exerciseId) ?: return@forEach
        exercises[id] = Exercise(
            id = id,
            name = v.variantName,
            baseName = v.exerciseName.takeIf { it != v.variantName }.orEmpty(),
            manufacturer = v.manufacturer.orEmpty(),
            muscleGroup = "Other",
            defaultIncrementKg = v.weightIncrementKg?.toFloat()?.takeIf { it.isFinite() && it > 0f } ?: 2.5f,
            seatPosition = v.setup?.seat.orEmpty(),
            handlePosition = v.setup?.handle.orEmpty(),
            notes = v.setup?.notes.orEmpty(),
            source = "user"
        )
    }
    // Workouts may name an exercise the variant list does not carry.
    fun exerciseFor(ref: String, name: String): Long? {
        val id = legacyId(ref) ?: return null
        exercises.getOrPut(id) { Exercise(id = id, name = name, muscleGroup = "Other", source = "user") }
        return id
    }

    val gyms = export.gyms.mapNotNull { g ->
        legacyId(g.id)?.let { Gym(id = it, name = g.name, notes = g.notes.orEmpty(), source = "user") }
    }

    val programs = mutableListOf<Program>()
    val programDays = mutableListOf<ProgramDay>()
    val programExercises = mutableListOf<ProgramExercise>()
    export.program?.let { p ->
        programs += Program(id = 1, name = p.name, isActive = true, source = "user")
        p.days.forEach { d ->
            val dayId = legacyId(d.id) ?: return@forEach
            val index = (d.sequence - 1).coerceAtLeast(0)
            programDays += ProgramDay(
                id = dayId, programId = 1, dayIndex = index, dayCode = DayCodes[index % DayCodes.size],
                title = d.name, isRestDay = d.type.equals("rest", ignoreCase = true)
            )
            d.exercises.forEach { pe ->
                val exerciseId = exerciseFor(pe.exerciseId, pe.name) ?: return@forEach
                programExercises += ProgramExercise(
                    id = programExercises.size + 1L, programDayId = dayId, exerciseId = exerciseId,
                    orderIndex = pe.position, targetWorkSets = pe.workSets.coerceAtLeast(1),
                    repMin = pe.repMin ?: 10, repMax = pe.repMax ?: 12,
                    targetRir = pe.rirMax?.toInt() ?: 0, restSeconds = pe.restSecondsMin ?: 180
                )
            }
        }
    }

    val readinessByWorkout = export.readiness.filter { it.workoutId != null }.associateBy { it.workoutId }
    val sessions = mutableListOf<WorkoutSession>()
    val workoutExercises = mutableListOf<WorkoutExercise>()
    val sets = mutableListOf<WorkoutSet>()
    val segments = mutableListOf<SetSegment>()
    export.workouts.forEach { w ->
        val sessionId = legacyId(w.id) ?: return@forEach
        val startedAt = time(w.startedAt) ?: return@forEach
        val finishedAt = time(w.finishedAt)
        val readiness = readinessByWorkout[w.id]
        sessions += WorkoutSession(
            id = sessionId,
            programDayId = legacyId(w.programDayId),
            gymId = legacyId(w.gymId),
            title = w.programDayName?.ifBlank { null } ?: "Workout",
            startedAt = startedAt,
            finishedAt = finishedAt,
            durationMinutes = finishedAt?.let { ((it - startedAt) / 60_000L).toInt().coerceAtLeast(0) } ?: 0,
            bodyweight = w.bodyweightKg?.toFloat() ?: 0f,
            sleepScore = readiness?.sleepQuality ?: 4,
            energyScore = readiness?.energy ?: 4,
            sorenessScore = readiness?.soreness ?: 2,
            motivationScore = readiness?.motivation ?: 5,
            jointDiscomfort = JointLabels.getOrElse(readiness?.jointDiscomfort ?: 0) { "None" },
            readinessStatus = readiness?.notes ?: "Normal",
            jointArea = readiness?.jointAreas.orEmpty().joinToString(", "),
            readinessNote = readiness?.note.orEmpty()
        )
        w.exercises.forEach { we ->
            val exerciseId = exerciseFor(we.exerciseId, we.name) ?: return@forEach
            val weId = workoutExercises.size + 1L
            val first = we.sets.firstOrNull()
            workoutExercises += WorkoutExercise(
                id = weId, workoutSessionId = sessionId, exerciseId = exerciseId, orderIndex = we.position,
                notes = we.setup?.notes.orEmpty(),
                seatPosition = we.setup?.seat.orEmpty(), handlePosition = we.setup?.handle.orEmpty(),
                executionQuality = first?.executionQuality?.capitalized() ?: "Good",
                targetMuscleQuality = first?.targetMuscleQuality?.capitalized() ?: "Good",
                targetWorkSets = we.target?.workSets?.coerceAtLeast(1),
                targetRepMin = we.target?.repMin, targetRepMax = we.target?.repMax,
                targetRir = we.target?.rirMax?.toInt(), restSeconds = we.target?.restSecondsMin
            )
            we.sets.forEach { s ->
                val setId = sets.size + 1L
                sets += WorkoutSet(
                    id = setId, workoutExerciseId = weId, setNumber = s.sequence,
                    setType = if (s.type.equals("warmup", ignoreCase = true)) SetType.WARMUP else SetType.WORK,
                    weightKg = s.weightKg?.toFloat() ?: Float.NaN,
                    reps = s.reps ?: 0,
                    rir = s.rir?.toInt() ?: 0,
                    technique = runCatching { IntensityTechnique.valueOf(s.technique.orEmpty().uppercase()) }
                        .getOrDefault(IntensityTechnique.NONE),
                    completedAt = time(s.performedAt) ?: startedAt
                )
                s.segments.forEach { seg ->
                    segments += SetSegment(
                        id = segments.size + 1L, workoutSetId = setId, segmentIndex = seg.sequence,
                        type = seg.type.uppercase(), weightKg = seg.weightKg?.toFloat() ?: Float.NaN,
                        reps = seg.reps ?: 0, restSeconds = seg.restSeconds ?: 20
                    )
                }
            }
        }
    }

    val cardio = export.cardio.mapIndexedNotNull { i, c ->
        val at = time(c.startedAt) ?: return@mapIndexedNotNull null
        CardioSession(
            id = i + 1L,
            workoutSessionId = legacyId(c.workoutId),
            machine = c.type.split(' ').joinToString(" ") { it.capitalized() },
            durationMinutes = (c.durationSeconds ?: 0) / 60,
            level = c.level?.toInt() ?: 0,
            avgHeartRate = c.avgHeartRate ?: 0,
            rpe = c.rpe?.toInt() ?: 0,
            timestamp = at
        )
    }

    return BackupExport(
        databaseVersion = databaseVersion,
        exercises = exercises.values.toList(),
        gyms = gyms,
        programs = programs,
        programDays = programDays,
        programExercises = programExercises,
        workoutSessions = sessions,
        workoutExercises = workoutExercises,
        workoutSets = sets,
        setSegments = segments,
        cardioSessions = cardio
    )
}

/** "ws-12" → 12. v1 ids are the database id behind a type prefix. */
internal fun legacyId(ref: String?): Long? = ref?.substringAfterLast('-')?.toLongOrNull()

private val DayCodes = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val JointLabels = listOf("None", "Mild", "Moderate", "Severe")

private fun String.capitalized(): String = lowercase().replaceFirstChar { it.titlecase(Locale.US) }
