package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

/** Bump together with a migration; also written into exports so Import knows the source schema. */
const val MUTANT_DB_VERSION = 8

@Database(
    entities = [
        MuscleGroup::class,
        MachineCatalogEntity::class,
        Gym::class,
        GymEquipmentEntity::class,
        Exercise::class,
        ExerciseVariant::class,
        Program::class,
        ProgramDay::class,
        ProgramExercise::class,
        WorkoutSession::class,
        WorkoutExercise::class,
        WorkoutSet::class,
        SetSegment::class,
        CardioSession::class
    ],
    version = MUTANT_DB_VERSION,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MutantDatabase : RoomDatabase() {

    abstract fun mutantDao(): MutantDao

    companion object {
        @Volatile
        private var INSTANCE: MutantDatabase? = null
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN updatedAt INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN currentExerciseIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restDeadline INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restRemainingSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restRecommended TEXT NOT NULL DEFAULT '2-3 min'")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN nextSetWeightKg REAL")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN nextSetReps INTEGER")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE programs ADD COLUMN currentRoutinePosition INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE programs ADD COLUMN lastCompletedSessionId INTEGER")
                db.execSQL("ALTER TABLE programs ADD COLUMN lastCompletedAt INTEGER")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetWorkSets INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetRepMin INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetRepMax INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN targetRir INTEGER")
                db.execSQL("ALTER TABLE workout_exercises ADD COLUMN restSeconds INTEGER")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN restTotalSeconds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessScore INTEGER")
                // Active sessions keep following their program day.
                db.execSQL(
                    """
                    UPDATE workout_exercises SET
                        targetWorkSets = (SELECT pe.targetWorkSets FROM program_exercises pe JOIN workout_sessions s ON s.programDayId = pe.programDayId
                            WHERE s.id = workout_exercises.workoutSessionId AND pe.exerciseId = workout_exercises.exerciseId LIMIT 1),
                        targetRepMin = (SELECT pe.repMin FROM program_exercises pe JOIN workout_sessions s ON s.programDayId = pe.programDayId
                            WHERE s.id = workout_exercises.workoutSessionId AND pe.exerciseId = workout_exercises.exerciseId LIMIT 1),
                        targetRepMax = (SELECT pe.repMax FROM program_exercises pe JOIN workout_sessions s ON s.programDayId = pe.programDayId
                            WHERE s.id = workout_exercises.workoutSessionId AND pe.exerciseId = workout_exercises.exerciseId LIMIT 1),
                        targetRir = (SELECT pe.targetRir FROM program_exercises pe JOIN workout_sessions s ON s.programDayId = pe.programDayId
                            WHERE s.id = workout_exercises.workoutSessionId AND pe.exerciseId = workout_exercises.exerciseId LIMIT 1),
                        restSeconds = (SELECT pe.restSeconds FROM program_exercises pe JOIN workout_sessions s ON s.programDayId = pe.programDayId
                            WHERE s.id = workout_exercises.workoutSessionId AND pe.exerciseId = workout_exercises.exerciseId LIMIT 1)
                    WHERE workoutSessionId IN (SELECT id FROM workout_sessions WHERE finishedAt IS NULL)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercises ADD COLUMN loadUnit TEXT NOT NULL DEFAULT 'KG'")
                db.execSQL("ALTER TABLE programs ADD COLUMN scheduleMode TEXT NOT NULL DEFAULT 'WEEKDAYS'")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN isRetroactive INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN jointArea TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN readinessNote TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): MutantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MutantDatabase::class.java,
                    "mutant_os_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
