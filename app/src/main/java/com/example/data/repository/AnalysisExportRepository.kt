package com.example.data.repository

import com.example.data.db.MUTANT_DB_VERSION
import com.example.data.db.MutantDao
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

interface AnalysisExportRepository {
    suspend fun buildExport(): AnalysisExport
}

class AnalysisExportRepositoryImpl(
    private val dao: MutantDao
) : AnalysisExportRepository {

    override suspend fun buildExport(): AnalysisExport {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val nowIso = isoFormat.format(Date())

        val programDays = dao.getAllProgramDaysSync()
        val programExercises = dao.getAllProgramExercisesSync()
        val exercises = dao.getAllExercisesSync()
        val exerciseMap = exercises.associateBy { it.id }
        val gyms = dao.getAllGymsSync()
        val workouts = dao.getAllWorkoutSessionsSync()
        val workoutExercises = dao.getAllWorkoutExercisesSync()
        val workoutSets = dao.getAllWorkoutSetsSync()
        val setSegments = dao.getAllSetSegmentsSync()
        val cardioList = dao.getAllCardioSessionsSync()

        val segmentsBySetId = setSegments.groupBy { it.workoutSetId }
        val setsByWorkoutExerciseId = workoutSets.groupBy { it.workoutExerciseId }
        val exercisesByWorkoutId = workoutExercises.groupBy { it.workoutSessionId }
        val programExercisesByDayId = programExercises.groupBy { it.programDayId }

        val appExport = AppExport(
            name = "Mutant Log",
            version = "1.0.0",
            databaseVersion = MUTANT_DB_VERSION
        )

        val profileExport = ProfileExport(
            unitSystem = "metric",
            timezone = TimeZone.getDefault().id
        )

        val gymsExport = gyms.map { g ->
            GymExport(
                id = "gym-${g.id}",
                name = g.name,
                notes = g.notes.ifEmpty { null }
            )
        }

        val programDaysExport = programDays.map { day ->
            val dayExercises = programExercisesByDayId[day.id] ?: emptyList()
            ProgramDayExport(
                id = "day-${day.id}",
                name = day.title,
                sequence = day.dayIndex + 1,
                type = if (day.isRestDay) "rest" else "training",
                exercises = dayExercises.map { pe ->
                    val ex = exerciseMap[pe.exerciseId]
                    ProgramExerciseExport(
                        exerciseId = "ex-${pe.exerciseId}",
                        variantId = "var-${pe.exerciseId}",
                        name = ex?.name ?: "Exercise ${pe.exerciseId}",
                        position = pe.orderIndex,
                        workSets = pe.targetWorkSets,
                        repMin = pe.repMin,
                        repMax = pe.repMax,
                        rirMin = 0.0,
                        rirMax = pe.targetRir.toDouble(),
                        restSecondsMin = pe.restSeconds,
                        restSecondsMax = pe.restSeconds,
                        notes = ex?.notes?.ifEmpty { null }
                    )
                }
            )
        }

        val programExport = ProgramExport(
            id = "program-nick-walker-reconstruction",
            name = "Nick Walker Reconstruction",
            startedAt = workouts.firstOrNull()?.startedAt?.let { isoFormat.format(Date(it)) },
            days = programDaysExport
        )

        val variantsExport = exercises.map { ex ->
            ExerciseVariantExport(
                id = "var-${ex.id}",
                exerciseId = "ex-${ex.id}",
                exerciseName = ex.baseName.ifEmpty { ex.name },
                variantName = ex.name,
                manufacturer = ex.manufacturer.ifEmpty { null },
                equipmentName = ex.name,
                gymId = null,
                resistanceType = when {
                    ex.name.contains("Machine", ignoreCase = true) -> "machine"
                    ex.name.contains("Cable", ignoreCase = true) -> "cable"
                    ex.name.contains("Dumbbell", ignoreCase = true) -> "free_weight_dumbbell"
                    ex.name.contains("Barbell", ignoreCase = true) -> "free_weight_barbell"
                    else -> "selectorized"
                },
                weightIncrementKg = ex.defaultIncrementKg.toDouble(),
                setup = SetupExport(
                    seat = ex.seatPosition.ifEmpty { null },
                    handle = ex.handlePosition.ifEmpty { null },
                    notes = ex.notes.ifEmpty { null }
                )
            )
        }

        val workoutsExport = workouts.map { session ->
            val wExercises = exercisesByWorkoutId[session.id] ?: emptyList()
            WorkoutExport(
                id = "ws-${session.id}",
                programDayId = session.programDayId?.let { "day-$it" },
                programDayName = session.title,
                gymId = session.gymId?.let { "gym-$it" },
                startedAt = isoFormat.format(Date(session.startedAt)),
                finishedAt = session.finishedAt?.let { isoFormat.format(Date(it)) },
                bodyweightKg = if (session.bodyweight > 0f) session.bodyweight.toDouble() else null,
                exercises = wExercises.map { we ->
                    val ex = exerciseMap[we.exerciseId]
                    val wSets = setsByWorkoutExerciseId[we.id] ?: emptyList()
                    val pEx = programExercises.find { it.programDayId == session.programDayId && it.exerciseId == we.exerciseId }

                    WorkoutExerciseExport(
                        exerciseId = "ex-${we.exerciseId}",
                        variantId = "var-${we.exerciseId}",
                        name = ex?.name ?: "Exercise ${we.exerciseId}",
                        position = we.orderIndex,
                        target = ExerciseTargetExport(
                            workSets = pEx?.targetWorkSets ?: ex?.defaultWorkSets,
                            repMin = pEx?.repMin ?: ex?.defaultRepMin,
                            repMax = pEx?.repMax ?: ex?.defaultRepMax,
                            rirMin = 0.0,
                            rirMax = (pEx?.targetRir ?: ex?.defaultRir ?: 0).toDouble(),
                            restSecondsMin = pEx?.restSeconds ?: ex?.defaultRestSeconds,
                            restSecondsMax = pEx?.restSeconds ?: ex?.defaultRestSeconds
                        ),
                        setup = SetupExport(
                            seat = we.seatPosition.ifEmpty { ex?.seatPosition?.ifEmpty { null } },
                            handle = we.handlePosition.ifEmpty { ex?.handlePosition?.ifEmpty { null } },
                            notes = we.notes.ifEmpty { null }
                        ),
                        sets = wSets.map { s ->
                            val segs = segmentsBySetId[s.id] ?: emptyList()
                            WorkoutSetExport(
                                sequence = s.setNumber,
                                type = when (s.setType) {
                                    SetType.WARMUP -> "warmup"
                                    SetType.WORK -> "work"
                                },
                                side = null,
                                weightKg = s.weightKg.toDouble(),
                                reps = s.reps,
                                rir = s.rir.toDouble(),
                                executionQuality = we.executionQuality.lowercase().ifEmpty { "good" },
                                targetMuscleQuality = we.targetMuscleQuality.lowercase().ifEmpty { "good" },
                                performedAt = isoFormat.format(Date(s.completedAt)),
                                restAfterSeconds = ex?.defaultRestSeconds,
                                technique = s.technique.name.lowercase(),
                                segments = segs.map { seg ->
                                    SetSegmentExport(
                                        sequence = seg.segmentIndex,
                                        type = seg.type.lowercase(),
                                        weightKg = seg.weightKg.toDouble(),
                                        reps = seg.reps,
                                        rir = null,
                                        restSeconds = seg.restSeconds
                                    )
                                },
                                notes = null
                            )
                        }
                    )
                }
            )
        }

        val bodyweightExport = workouts.filter { it.bodyweight > 0f }.map { session ->
            BodyweightExport(
                measuredAt = isoFormat.format(Date(session.finishedAt ?: session.startedAt)),
                weightKg = session.bodyweight.toDouble()
            )
        }

        val readinessExport = workouts.map { session ->
            ReadinessExport(
                date = dateFormat.format(Date(session.startedAt)),
                workoutId = "ws-${session.id}",
                sleepHours = null,
                sleepQuality = session.sleepScore,
                energy = session.energyScore,
                motivation = session.motivationScore,
                soreness = session.sorenessScore,
                jointDiscomfort = when (session.jointDiscomfort.lowercase()) {
                    "mild" -> 1
                    "moderate" -> 2
                    "severe" -> 3
                    else -> 0
                },
                stress = null,
                notes = session.readinessStatus.ifEmpty { null }
            )
        }

        val cardioExport = cardioList.map { c ->
            CardioExport(
                startedAt = isoFormat.format(Date(c.timestamp)),
                workoutId = c.workoutSessionId?.let { "ws-$it" },
                type = c.machine.lowercase(),
                durationSeconds = c.durationMinutes * 60,
                level = c.level.toDouble(),
                incline = null,
                speed = null,
                distanceKm = null,
                avgHeartRate = if (c.avgHeartRate > 0) c.avgHeartRate else null,
                maxHeartRate = null,
                rpe = c.rpe.toDouble()
            )
        }

        // Lossless copy for Import. Finished sessions only: an active session is device-local state.
        val finished = workouts.filter { it.finishedAt != null }
        val finishedIds = finished.mapTo(HashSet()) { it.id }
        val backupExercises = workoutExercises.filter { it.workoutSessionId in finishedIds }
        val backupExerciseIds = backupExercises.mapTo(HashSet()) { it.id }
        val backupSets = workoutSets.filter { it.workoutExerciseId in backupExerciseIds }
        val backupSetIds = backupSets.mapTo(HashSet()) { it.id }
        val backup = BackupExport(
            databaseVersion = MUTANT_DB_VERSION,
            exercises = dao.getEveryExerciseSync(),
            gyms = dao.getEveryGymSync(),
            gymEquipment = dao.getAllGymEquipmentSync(),
            machineCatalog = dao.getUserMachinesSync(),
            exerciseVariants = dao.getUserExerciseVariantsSync(),
            programs = dao.getAllProgramsSync(),
            programDays = programDays,
            programExercises = programExercises,
            workoutSessions = finished,
            workoutExercises = backupExercises,
            workoutSets = backupSets,
            setSegments = setSegments.filter { it.workoutSetId in backupSetIds },
            cardioSessions = cardioList.filter { it.workoutSessionId == null || it.workoutSessionId in finishedIds }
        )

        return AnalysisExport(
            schemaVersion = EXPORT_SCHEMA_VERSION,
            generatedAt = nowIso,
            app = appExport,
            profile = profileExport,
            program = programExport,
            gyms = gymsExport,
            exerciseVariants = variantsExport,
            workouts = workoutsExport,
            bodyweight = bodyweightExport,
            cardio = cardioExport,
            readiness = readinessExport,
            derived = DerivedExport(),
            backup = backup
        )
    }
}
