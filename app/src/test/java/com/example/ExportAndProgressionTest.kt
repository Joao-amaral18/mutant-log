package com.example

import com.example.data.model.Exercise
import com.example.data.model.HistoryExercise
import com.example.data.model.HistorySet
import com.example.data.model.LOAD_UNIT_STACK
import com.example.data.model.ProgressionEngine
import com.example.data.model.ProgressionStatus
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutSet
import com.example.data.model.progressionStepKg
import com.example.data.repository.actualRestSeconds
import com.example.data.repository.resistanceTypeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportAndProgressionTest {
    private fun set(n: Int, kg: Float, reps: Int, rir: Int, at: Long = 0L) =
        WorkoutSet(id = n.toLong(), workoutExerciseId = 1, setNumber = n, weightKg = kg, reps = reps, rir = rir, completedAt = at)

    @Test fun `rest is the measured gap to the next set`() {
        assertEquals(150, actualRestSeconds(false, set(1, 20f, 10, 0, 1_000), set(2, 20f, 10, 0, 151_000)))
        assertNull(actualRestSeconds(false, set(2, 20f, 10, 0, 1_000), null))
        assertNull(actualRestSeconds(true, set(1, 20f, 10, 0, 1_000), set(2, 20f, 10, 0, 5_000)))
    }

    @Test fun `resistance type follows the load unit and equipment`() {
        assertEquals("selectorized", resistanceTypeOf(Exercise(name = "Lat Pulldown", muscleGroup = "Back", loadUnit = LOAD_UNIT_STACK)))
        assertEquals("plate_loaded", resistanceTypeOf(Exercise(name = "Horizontal Chest Press — Hammer Strength", muscleGroup = "Chest", manufacturer = "Hammer Strength")))
        assertEquals("cable", resistanceTypeOf(Exercise(name = "Cable Curl", muscleGroup = "Biceps")))
    }

    @Test fun `progression builds on the heaviest set and ignores lighter back-offs`() {
        val sets = listOf(set(1, 100f, 12, 0), set(2, 80f, 9, 0))
        val rec = ProgressionEngine.computeProgression(sets, repMin = 10, repMax = 12, targetRir = 0, incrementKg = 5f)
        assertEquals(ProgressionStatus.INCREASE_LOAD, rec.status)
        assertEquals(105f, rec.suggestedWeightKg, 0.01f)
        val heaviestLater = ProgressionEngine.computeProgression(listOf(set(1, 80f, 12, 0), set(2, 100f, 9, 0)), repMin = 10, repMax = 12, incrementKg = 5f)
        assertEquals(ProgressionStatus.MAINTAIN_LOAD, heaviestLater.status)
        assertEquals(100f, heaviestLater.suggestedWeightKg, 0.01f)
    }

    @Test fun `RIR one on every set of a zero RIR plan holds the load`() {
        val sets = listOf(set(1, 25f, 12, 1), set(2, 25f, 12, 1))
        val rec = ProgressionEngine.computeProgression(sets, repMin = 10, repMax = 12, targetRir = 0, incrementKg = 2.5f)
        assertEquals(ProgressionStatus.MAINTAIN_LOAD, rec.status)
    }

    @Test fun `stack step is one pin unless the increment was customised`() {
        assertEquals(1f, Exercise(name = "Pulldown", muscleGroup = "Back", loadUnit = LOAD_UNIT_STACK).progressionStepKg, 0f)
        assertEquals(2f, Exercise(name = "Pulldown", muscleGroup = "Back", loadUnit = LOAD_UNIT_STACK, defaultIncrementKg = 2f).progressionStepKg, 0f)
        assertEquals(2.5f, Exercise(name = "Press", muscleGroup = "Chest").progressionStepKg, 0f)
    }

    @Test fun `stack work shows as pin volume and stays out of kg volume`() {
        val ex = HistoryExercise(WorkoutExercise(workoutSessionId = 1, exerciseId = 1, orderIndex = 0),
            Exercise(name = "Pulldown", muscleGroup = "Back", loadUnit = LOAD_UNIT_STACK), listOf(HistorySet(set(1, 8f, 12, 0))))
        assertEquals(0.0, ex.volume, 0.0)
        assertEquals(96.0, ex.pinVolume, 0.0)
    }
}
