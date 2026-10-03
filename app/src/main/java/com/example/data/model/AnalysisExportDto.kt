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
    val exerciseVariants: List<ExerciseVariantExport>,

    val workouts: List<WorkoutExport>,

    val bodyweight: List<BodyweightExport>,
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
    val note: String = "Computed metrics (PRs, fatigue, progression) are derived and can be recalculated from raw data"
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
    val variantId: String,
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

    val exercises: List<WorkoutExerciseExport>
)

@Serializable
data class WorkoutExerciseExport(
    val exerciseId: String,
    val variantId: String,

    val name: String,

    val position: Int,

    val target: ExerciseTargetExport?,

    val setup: SetupExport?,

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

    val restAfterSeconds: Int?,

    val technique: String?,

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

    val notes: String?
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
