package com.example

import com.example.data.model.WarmupPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class WarmupPlanTest {
    @Test fun warmupIsFortyFivePercentRoundedToIncrement() {
        assertEquals(35f, WarmupPlan.warmupLoad(80f, 2.5f, false), 0.001f)
    }

    @Test fun prepsAreSeventyFiveAndNinetyPercent() {
        assertEquals(60f, WarmupPlan.prepLoad(80f, 0, 2.5f, false), 0.001f)
        assertEquals(72.5f, WarmupPlan.prepLoad(80f, 1, 2.5f, false), 0.001f)
    }

    @Test fun stackLoadsMoveInWholePins() {
        assertEquals(7f, WarmupPlan.prepLoad(9f, 0, 2.5f, true), 0.001f)
        assertEquals(4f, WarmupPlan.warmupLoad(9f, 2.5f, true), 0.001f)
    }
}
