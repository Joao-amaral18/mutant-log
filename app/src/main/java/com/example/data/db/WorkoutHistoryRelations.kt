package com.example.data.db

import androidx.room.*
import com.example.data.model.*

data class HistorySetRelation(
    @Embedded val set: WorkoutSet,
    @Relation(parentColumn = "id", entityColumn = "workoutSetId") val segments: List<SetSegment>
)

data class HistoryExerciseRelation(
    @Embedded val workoutExercise: WorkoutExercise,
    @Relation(parentColumn = "exerciseId", entityColumn = "id") val exercise: Exercise,
    @Relation(entity = WorkoutSet::class, parentColumn = "id", entityColumn = "workoutExerciseId") val sets: List<HistorySetRelation>
)

data class HistoryWorkoutRelation(
    @Embedded val session: WorkoutSession,
    @Relation(entity = WorkoutExercise::class, parentColumn = "id", entityColumn = "workoutSessionId") val exercises: List<HistoryExerciseRelation>,
    @Relation(parentColumn = "id", entityColumn = "workoutSessionId") val cardio: List<CardioSession>
) {
    fun toHistory() = HistoryWorkout(session, exercises.sortedBy { it.workoutExercise.orderIndex }.map { ex ->
        HistoryExercise(ex.workoutExercise, ex.exercise, ex.sets.sortedBy { it.set.setNumber }.map { HistorySet(it.set, it.segments.sortedBy { s -> s.segmentIndex }) })
    }, cardio)
}
