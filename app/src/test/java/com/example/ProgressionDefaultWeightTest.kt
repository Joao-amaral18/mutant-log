package com.example

import com.example.data.model.ProgressionEngine
import com.example.data.model.ProgressionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionDefaultWeightTest {
    @Test
    fun firstTimeSuggestsZeroLoad() {
        val result = ProgressionEngine.computeProgression(emptyList())
        assertEquals(ProgressionStatus.FIRST_TIME, result.status)
        assertEquals(0f, result.suggestedWeightKg, 0f)
    }
}
