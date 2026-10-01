package com.example.data.db

import com.example.data.model.SetType

/** Stored in targetWorkSets for an exercise added mid-session: it has no plan and takes as many sets as logged. */
const val AD_HOC_TARGET_SETS = 0

val WorkoutExerciseDetail.isAdHoc: Boolean get() = workoutExercise.targetWorkSets == AD_HOC_TARGET_SETS

/** The plan for this session: program-day targets when present, otherwise the exercise defaults. Zero when ad hoc. */
val WorkoutExerciseDetail.plannedSets: Int get() =
    if (isAdHoc) 0 else (workoutExercise.targetWorkSets ?: exercise.defaultWorkSets).coerceAtLeast(1)
val WorkoutExerciseDetail.repMin: Int get() = workoutExercise.targetRepMin ?: exercise.defaultRepMin
val WorkoutExerciseDetail.repMax: Int get() = (workoutExercise.targetRepMax ?: exercise.defaultRepMax).coerceAtLeast(repMin)
val WorkoutExerciseDetail.plannedRir: Int get() = workoutExercise.targetRir ?: exercise.defaultRir
val WorkoutExerciseDetail.plannedRestSeconds: Int get() = workoutExercise.restSeconds ?: exercise.defaultRestSeconds

val WorkoutExerciseDetail.workSetCount: Int get() = sets.count { it.setType == SetType.WORK }

/** Sets this exercise contributes to session progress: the plan, or what was logged when ad hoc. */
val WorkoutExerciseDetail.sessionSets: Int get() = if (isAdHoc) workSetCount else plannedSets

/** Planned sets are met. Ad-hoc exercises are never "done"; the user moves on when ready. */
val WorkoutExerciseDetail.isComplete: Boolean get() = !isAdHoc && workSetCount >= plannedSets
