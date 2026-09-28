package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

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
    version = 5,
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

        fun getDatabase(context: Context): MutantDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MutantDatabase::class.java,
                    "mutant_os_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
