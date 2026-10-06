package com.example.data.model

import kotlinx.serialization.Serializable

/** v1: analysis sections only. v2 adds [AnalysisExport.backup], a lossless copy used by Import. */
const val EXPORT_SCHEMA_VERSION = 2

@Serializable
data class AnalysisExport(
    val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
    val generatedAt: String,

    val app: AppExport,
    val profile: ProfileExport?,

    val program: ProgramExport?,

    val gyms: List<GymExport>,
    // Equipment variants actually referenced by a workout or program exercise; ids match WorkoutExerciseExport.variantId.
    val exerciseVariants: List<ExerciseVariantExport>,
    // Every exercise with its muscle, load unit and equipment details; ids match WorkoutExerciseExport.exerciseId.
    val exercises: List<ExerciseExport> = emptyList(),

    val workouts: List<WorkoutExport>,

    // Bodyweight lives on each workout (WorkoutExport.bodyweightKg); this list stays empty and is kept so older readers still parse.
    val bodyweight: List<BodyweightExport> = emptyList(),
    val cardio: List<CardioExport>,
    val readiness: List<ReadinessExport>,

    val derived: DerivedExport? = null,

    // Raw rows for restore. Absent in v1 files, which are converted by legacyToBackup.
    val backup: BackupExport? = null
)

/**
 * Database rows as stored, keyed by their original ids. Import never reuses these ids; it remaps them.
 * Only finished sessions are included, so no rest-timer state is carried across devices.
 */
@Serializable
data class BackupExport(
    val databaseVersion: Int,
    val exercises: List<Exercise> = emptyList(),
    val gyms: List<Gym> = emptyList(),
    val gymEquipment: List<GymEquipmentEntity> = emptyList(),
    val machineCatalog: List<MachineCatalogEntity> = emptyList(),
    val exerciseVariants: List<ExerciseVariant> = emptyList(),
    val programs: List<Program> = emptyList(),
    val programDays: List<ProgramDay> = emptyList(),
    val programExercises: List<ProgramExercise> = emptyList(),
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val workoutExercises: List<WorkoutExercise> = emptyList(),
    val workoutSets: List<WorkoutSet> = emptyList(),
    val setSegments: List<SetSegment> = emptyList(),
    val cardioSessions: List<CardioSession> = emptyList()
)

@Serializable
data class DerivedExport(
    val note: String = "Computed from the raw workouts above; recalculate rather than edit.",
    val exerciseSessions: List<DerivedExerciseSession> = emptyList(),
    val weeklyMuscleSets: List<DerivedWeeklyMuscleSets> = emptyList(),
    val exerciseBests: List<DerivedExerciseBest> = emptyList()
)

/** One exercise in one finished workout, work sets only (drop-set and rest-pause segments add to reps and volume). */
@Serializable
data class DerivedExerciseSession(
    val workoutId: String,
    val exerciseId: String,
    val date: String,
    val loadUnit: String,
    val workSets: Int,
    val totalReps: Int,
    val topWeightKg: Double,
    val topReps: Int,
    // Epley on reps-to-failure (reps + RIR). For stack_pin exercises the unit is pin positions.
    val bestE1rm: Double,
    // kg × reps; null for stack_pin exercises.
    val volumeKg: Double?,
    // pin × reps; null for kg exercises.
    val pinVolume: Double?
)

/** Work sets per primary muscle per ISO week (local time), machine work included. */
@Serializable
data class DerivedWeeklyMuscleSets(
    val week: String,
    val muscle: String,
    val workSets: Int
)

@Serializable
data class DerivedExerciseBest(
    val exerciseId: String,
    val loadUnit: String,
    val bestE1rm: Double,
    val bestE1rmDate: String,
    val sessions: Int
)

@Serializable
data class ExerciseExport(
    val id: String,
    val name: String,
    val baseName: String?,
    val manufacturer: String?,
    val muscleGroup: String,
    val secondaryMuscles: List<String> = emptyList(),
    // "kg", or "stack_pin" when loads are pin positions.
    val loadUnit: String,
    val resistanceType: String,
    val weightIncrementKg: Double,
    val setup: SetupExport?
)

@Serializable
data class AppExport(
    val name: String = "Mutant Log",
    val version: String = "1.0.0",
    val databaseVersion: Int = 1
)

@Serializable
data class ProfileExport(
    val unitSystem: String = "metric",
    val timezone: String
)

@Serializable
data class GymExport(
    val id: String,
    val name: String,
    val notes: String?
)

@Serializable
data class ProgramExport(
    val id: String,
    val name: String,
    val startedAt: String?,
    val days: List<ProgramDayExport>
)

@Serializable
data class ProgramDayExport(
    val id: String,
    val name: String,
    val sequence: Int,
    val type: String,
    val exercises: List<ProgramExerciseExport>
)

@Serializable
data class ProgramExerciseExport(
    val exerciseId: String,
    // The equipment variant picked for this slot, or null when none was picked.
    val variantId: String?,
    val name: String,

    val position: Int,

    val workSets: Int,

    val repMin: Int?,
    val repMax: Int?,

    val rirMin: Double?,
    val rirMax: Double?,

    val restSecondsMin: Int?,
    val restSecondsMax: Int?,

    val notes: String?
)

@Serializable
data class ExerciseVariantExport(
    val id: String,

    val exerciseId: String,
    val exerciseName: String,

    val variantName: String,

    val manufacturer: String?,
    val equipmentName: String?,
    val gymId: String?,

    val resistanceType: String?,

    val weightIncrementKg: Double?,

    val setup: SetupExport?
)

@Serializable
data class SetupExport(
    val seat: String?,
    val backrest: String? = null,
    val handle: String?,
    val footPosition: String? = null,
    val notes: String?
)

@Serializable
data class WorkoutExport(
    val id: String,

    val programDayId: String?,
    val programDayName: String?,

    val gymId: String?,

    val startedAt: String,
    val finishedAt: String?,

    val bodyweightKg: Double?,

    val exercises: List<WorkoutExerciseExport>,

    // Logged after the fact: start and duration were entered by hand and set times are spread over them.
    val retroactive: Boolean = false
)

@Serializable
data class WorkoutExerciseExport(
    val exerciseId: String,
    // The equipment variant used, or null when none was picked.
    val variantId: String?,

    val name: String,

    val position: Int,

    val target: ExerciseTargetExport?,

    val setup: SetupExport?,

    // Rated once per exercise; the per-set fields below are null.
    val executionQuality: String? = null,
    val targetMuscleQuality: String? = null,

    val sets: List<WorkoutSetExport>
)

@Serializable
data class ExerciseTargetExport(
    val workSets: Int?,
    val repMin: Int?,
    val repMax: Int?,
    val rirMin: Double?,
    val rirMax: Double?,
    val restSecondsMin: Int?,
    val restSecondsMax: Int?
)

@Serializable
data class WorkoutSetExport(
    val sequence: Int,

    val type: String,

    val side: String? = null,

    val weightKg: Double?,
    val reps: Int?,
    val rir: Double?,

    val executionQuality: String?,
    val targetMuscleQuality: String?,

    val performedAt: String?,

    // Measured gap to the next set of the same exercise; null for the last set and for retroactive sessions.
    val restAfterSeconds: Int?,
    // The rest the program prescribed, kept separate from what was measured.
    val plannedRestSeconds: Int? = null,

    val technique: String?,

    // "kg", or "stack_pin" when weightKg is a pin position on a selectorized stack rather than kilograms.
    val loadUnit: String = "kg",

    val segments: List<SetSegmentExport> = emptyList(),

    val notes: String? = null
)

@Serializable
data class SetSegmentExport(
    val sequence: Int,

    val type: String,

    val weightKg: Double?,
    val reps: Int?,

    val rir: Double?,

    val restSeconds: Int?
)

@Serializable
data class ReadinessExport(
    val date: String,

    val workoutId: String?,

    val sleepHours: Double?,
    val sleepQuality: Int?,
    val energy: Int?,
    val motivation: Int?,
    val soreness: Int?,
    val jointDiscomfort: Int?,
    val stress: Int?,

    // Computed readiness status ("Normal", "Accumulated fatigue detected").
    val notes: String?,

    // Every value here comes from the check-in before the workout started, not from the workout itself.
    val measured: String = "pre_session",
    // "none", "mild", "moderate" or "severe"; the same as jointDiscomfort 0–3.
    val jointDiscomfortLevel: String? = null,
    // Joints the user marked, e.g. ["Elbow"].
    val jointAreas: List<String> = emptyList(),
    // The user's own note, e.g. "elbow, since biceps day".
    val note: String? = null
)

@Serializable
data class BodyweightExport(
    val measuredAt: String,
    val weightKg: Double
)

@Serializable
data class CardioExport(
    val startedAt: String,

    val workoutId: String?,

    val type: String,

    val durationSeconds: Int?,

    val level: Double?,
    val incline: Double?,
    val speed: Double?,

    val distanceKm: Double?,

    val avgHeartRate: Int?,
    val maxHeartRate: Int?,

    val rpe: Double?
)
