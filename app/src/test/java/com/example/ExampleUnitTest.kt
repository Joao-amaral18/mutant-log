package com.example

import com.example.data.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionEngineTest {

    @Test
    fun doubleProgression_triggersIncrease_whenAllSetsHitTopBracketWithStrictExecution() {
        val workSets = listOf(
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 1,
                setType = SetType.WORK,
                weightKg = 90f,
                reps = 12,
                rir = 1
            ),
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 2,
                setType = SetType.WORK,
                weightKg = 90f,
                reps = 12,
                rir = 0
            )
        )

        val result = ProgressionEngine.computeProgression(
            lastWorkSets = workSets,
            repMin = 10,
            repMax = 12,
            targetRir = 0,
            incrementKg = 5f,
            lastExecutionQuality = "Good",
            lastTargetMuscleQuality = "Good"
        )

        assertEquals(ProgressionStatus.INCREASE_LOAD, result.status)
        assertEquals(95f, result.suggestedWeightKg, 0.01f)
    }

    @Test
    fun doubleProgression_maintainsWeight_whenAnyWorkSetIsBelowMaxReps() {
        val workSets = listOf(
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 1,
                setType = SetType.WORK,
                weightKg = 95f,
                reps = 12,
                rir = 0
            ),
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 2,
                setType = SetType.WORK,
                weightKg = 95f,
                reps = 9,
                rir = 0
            )
        )

        val result = ProgressionEngine.computeProgression(
            lastWorkSets = workSets,
            repMin = 10,
            repMax = 12,
            targetRir = 0,
            incrementKg = 2.5f,
            lastExecutionQuality = "Good",
            lastTargetMuscleQuality = "Good"
        )

        assertEquals(ProgressionStatus.MAINTAIN_LOAD, result.status)
        assertEquals(95f, result.suggestedWeightKg, 0.01f)
    }

    @Test
    fun doubleProgression_preventsIncrease_whenExecutionIsCompromised() {
        val workSets = listOf(
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 1,
                setType = SetType.WORK,
                weightKg = 100f,
                reps = 12,
                rir = 0
            ),
            WorkoutSet(
                workoutExerciseId = 1,
                setNumber = 2,
                setType = SetType.WORK,
                weightKg = 100f,
                reps = 12,
                rir = 0
            )
        )

        val result = ProgressionEngine.computeProgression(
            lastWorkSets = workSets,
            repMin = 10,
            repMax = 12,
            targetRir = 0,
            incrementKg = 5f,
            lastExecutionQuality = "Compromised",
            lastTargetMuscleQuality = "Good"
        )

        assertEquals(ProgressionStatus.DELOAD_OR_CONSOLIDATE, result.status)
        assertEquals(100f, result.suggestedWeightKg, 0.01f)
    }
}
