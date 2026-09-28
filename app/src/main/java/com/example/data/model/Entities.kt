package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// --- 1. REFERENCE DATA: Muscle Groups ---
@Entity(tableName = "muscle_groups")
data class MuscleGroup(
    @PrimaryKey val id: String, // e.g. "muscle_chest", "muscle_lats"
    val name: String,
    val region: String, // "Upper Body Push", "Upper Body Pull", "Legs", "Arms", "Core"
    val orderIndex: Int
)

// --- 2. REFERENCE DATA: Machine Catalog ---
@Entity(tableName = "machine_catalog")
data class MachineCatalogEntity(
    @PrimaryKey val id: String, // e.g. "machine_panatta_super_incline_bench"
    val manufacturer: String?, // "Panatta", "Hammer Strength", "Prime", "Arsenal", etc.
    val model: String, // e.g. "Super Inclined Chest Press"
    val equipmentType: String, // "Plate-Loaded", "Selectorized", "Cable", "Smith", "Free Weight"
    val defaultIncrementKg: Float = 2.5f,
    val source: String = "bundled", // "bundled" or "user"
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// --- 3. REFERENCE DATA: Exercise Canonical Library ---
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stableId: String = "", // e.g. "exercise_incline_chest_press"
    val name: String, // e.g. "Incline Machine Press — Panatta"
    val baseName: String = "", // e.g. "Incline Chest Press"
    val manufacturer: String = "", // "Panatta", "Hammer Strength", etc.
    val muscleGroup: String, // "Chest", "Back", "Quads", "Hamstrings", etc.
    val secondaryMuscles: String = "", // comma-separated
    val movementPattern: String = "Incline Press", // "Press", "Pulldown", "Row", "Squat", "Hinge", "Curl", etc.
    val defaultRepMin: Int = 10,
    val defaultRepMax: Int = 12,
    val defaultRir: Int = 0,
    val defaultWorkSets: Int = 2,
    val defaultRestSeconds: Int = 180,
    val defaultIncrementKg: Float = 2.5f,
    val executionCues: String = "",
    val seatPosition: String = "",
    val handlePosition: String = "",
    val notes: String = "",
    val source: String = "bundled", // "bundled" or "user"
    val isArchived: Boolean = false
)

// --- 4. REFERENCE DATA: Exercise Variants ---
@Entity(
    tableName = "exercise_variants",
    indices = [Index("exerciseId"), Index("machineCatalogId")]
)
data class ExerciseVariant(
    @PrimaryKey val id: String, // e.g. "var_incline_panatta"
    val exerciseId: Long,
    val exerciseStableId: String = "",
    val machineCatalogId: String? = null,
    val variantName: String, // e.g. "Panatta Super Incline Press", "Hammer Strength Incline"
    val manufacturer: String = "",
    val resistanceType: String = "Plate-Loaded", // "Plate-Loaded", "Selectorized", "Cable", "Smith", "Free Weight"
    val weightIncrementKg: Float = 2.5f,
    val defaultSeat: String = "",
    val defaultHandle: String = "",
    val defaultBackrest: String = "",
    val notes: String = "",
    val source: String = "bundled", // "bundled" or "user"
    val isArchived: Boolean = false
)

// --- 5. REFERENCE / USER DATA: Gyms ---
@Entity(tableName = "gyms")
data class Gym(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String = "",
    val source: String = "bundled", // "bundled" or "user"
    val isArchived: Boolean = false
)

// --- 6. GYM EQUIPMENT: Maps machine models to specific gyms ---
@Entity(
    tableName = "gym_equipment",
    foreignKeys = [
        ForeignKey(
            entity = Gym::class,
            parentColumns = ["id"],
            childColumns = ["gymId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MachineCatalogEntity::class,
            parentColumns = ["id"],
            childColumns = ["machineCatalogId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("gymId"), Index("machineCatalogId")]
)
data class GymEquipmentEntity(
    @PrimaryKey val id: String,
    val gymId: Long,
    val machineCatalogId: String?,
    val customName: String?,
    val notes: String?,
    val isActive: Boolean = true
)

// --- 7. USER DATA: Programs ---
@Entity(tableName = "programs")
data class Program(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "user",
    // Position of the last completed training day within the non-rest sequence.
    val currentRoutinePosition: Int = -1,
    val lastCompletedSessionId: Long? = null,
    val lastCompletedAt: Long? = null
)

@Entity(tableName = "program_days")
data class ProgramDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long = 0,
    val dayIndex: Int, // 0 = SEG, 1 = TER, 2 = QUA, 3 = QUI, 4 = SEX, 5 = SAB, 6 = DOM
    val dayCode: String, // "SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM"
    val title: String, // "Chest + Biceps", "Back", "Rest", etc.
    val isRestDay: Boolean = false,
    val description: String = ""
)

@Entity(
    tableName = "program_exercises",
    foreignKeys = [
        ForeignKey(
            entity = ProgramDay::class,
            parentColumns = ["id"],
            childColumns = ["programDayId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("programDayId"), Index("exerciseId")]
)
data class ProgramExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long,
    val exerciseId: Long,
    val variantId: String = "",
    val orderIndex: Int,
    val targetWorkSets: Int = 2,
    val repMin: Int = 10,
    val repMax: Int = 12,
    val targetRir: Int = 0,
    val restSeconds: Int = 180
)

// --- 8. USER DATA: Workout Sessions ---
@Entity(
    tableName = "workout_sessions",
    indices = [Index("programDayId"), Index("gymId")]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programDayId: Long? = null,
    val gymId: Long? = null,
    val title: String, // e.g. "Chest + Biceps"
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val durationMinutes: Int = 0,
    val bodyweight: Float = 0f,
    val notes: String = "",
    // Pre-workout Readiness
    val sleepScore: Int = 4, // 1-5
    val energyScore: Int = 4, // 1-5
    val sorenessScore: Int = 2, // 1-5
    val jointDiscomfort: String = "None", // None, Mild, Moderate, Severe
    val motivationScore: Int = 5, // 1-5
    val updatedAt: Long? = null,
    val currentExerciseIndex: Int = 0,
    val restDeadline: Long? = null,
    val restRemainingSeconds: Int = 0,
    val restCompleted: Boolean = false,
    val restRecommended: String = "2-3 min",
    val readinessStatus: String = "Normal" // "Normal", "High fatigue detected"
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutSessionId"), Index("exerciseId")]
)
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSessionId: Long,
    val exerciseId: Long,
    val variantId: String = "",
    val orderIndex: Int,
    val notes: String = "",
    val nextSetWeightKg: Float? = null,
    val nextSetReps: Int? = null,
    val seatPosition: String = "",
    val handlePosition: String = "",
    val executionQuality: String = "Good", // Excellent, Good, Compromised, Bad
    val targetMuscleQuality: String = "Good" // Excellent, Good, Weak, None
)

enum class SetType {
    WARMUP,
    WORK
}

enum class IntensityTechnique {
    NONE,
    REST_PAUSE,
    DROP_SET,
    ASSISTED_REPS,
    PARTIAL_REPS
}

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExercise::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId")]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val setType: SetType = SetType.WORK,
    val weightKg: Float,
    val reps: Int,
    val rir: Int = 0, // Reps in Reserve (0 = failure)
    val technique: IntensityTechnique = IntensityTechnique.NONE,
    val completedAt: Long = System.currentTimeMillis(),
    val isPr: Boolean = false,
    val prType: String = "" // "LOAD", "REP", "TECHNIQUE", ""
)

@Entity(
    tableName = "set_segments",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSet::class,
            parentColumns = ["id"],
            childColumns = ["workoutSetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutSetId")]
)
data class SetSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSetId: Long,
    val segmentIndex: Int,
    val type: String = "REST_PAUSE", // REST_PAUSE, DROP_SET
    val weightKg: Float,
    val reps: Int,
    val restSeconds: Int = 20
)

@Entity(tableName = "cardio_sessions")
data class CardioSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSessionId: Long? = null,
    val machine: String = "Stairmaster",
    val durationMinutes: Int = 20,
    val level: Int = 6,
    val avgHeartRate: Int = 140,
    val rpe: Int = 5,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

fun Float.formatLoad(): String = if (this % 1f == 0f) this.toInt().toString() else "%.1f".format(this)
fun Double.formatLoad(): String = if (this % 1.0 == 0.0) this.toInt().toString() else "%.1f".format(this)

