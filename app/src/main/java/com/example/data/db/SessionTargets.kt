package com.example.data.db

/** The plan for this session: program-day targets when present, otherwise the exercise defaults. */
val WorkoutExerciseDetail.plannedSets: Int get() = (workoutExercise.targetWorkSets ?: exercise.defaultWorkSets).coerceAtLeast(1)
val WorkoutExerciseDetail.repMin: Int get() = workoutExercise.targetRepMin ?: exercise.defaultRepMin
val WorkoutExerciseDetail.repMax: Int get() = (workoutExercise.targetRepMax ?: exercise.defaultRepMax).coerceAtLeast(repMin)
val WorkoutExerciseDetail.plannedRir: Int get() = workoutExercise.targetRir ?: exercise.defaultRir
val WorkoutExerciseDetail.plannedRestSeconds: Int get() = workoutExercise.restSeconds ?: exercise.defaultRestSeconds
