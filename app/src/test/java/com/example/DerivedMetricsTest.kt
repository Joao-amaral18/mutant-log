package com.example

import com.example.data.model.DerivedInput
import com.example.data.model.DerivedMetrics
import com.example.data.model.Exercise
import com.example.data.model.LOAD_UNIT_STACK
import com.example.data.model.SetType
import com.example.data.model.WorkoutSession
import com.example.data.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.TimeZone

class DerivedMetricsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    // 2026-09-28 (Monday, ISO week 40) and 2026-10-03 (Saturday, same week).
    private val mon = 1_790_589_600_000L
    private val sat = mon + 5 * 86_400_000L

    private fun session(id: Long, at: Long, finished: Boolean = true) =
        WorkoutSession(id = id, title = "W$id", startedAt = at, finishedAt = if (finished) at + 3_600_000 else null)

    private fun set(n: Int, kg: Float, reps: Int, rir: Int = 0, type: SetType = SetType.WORK) =
        WorkoutSet(id = n.toLong(), workoutExerciseId = 1, setNumber = n, setType = type, weightKg = kg, reps = reps, rir = rir)

    private val press = Exercise(id = 1, name = "Press", muscleGroup = "Chest")
    private val pulldown = Exercise(id = 2, name = "Pulldown", muscleGroup = "Back", loadUnit = LOAD_UNIT_STACK)

    @Test fun `iso week uses Monday starts`() {
        assertEquals("2026-W40", DerivedMetrics.isoWeek(mon, utc))
        assertEquals("2026-W40", DerivedMetrics.isoWeek(sat, utc))
    }

    @Test fun `exercise sessions split kg and pin volume and skip warmups`() {
        val d = DerivedMetrics.build(listOf(
            DerivedInput(session(1, mon), press, listOf(set(1, 20f, 10, type = SetType.WARMUP), set(2, 100f, 10, 1), set(3, 100f, 8))),
            DerivedInput(session(1, mon), pulldown, listOf(set(4, 8f, 12)))
        ), utc)
        val p = d.exerciseSessions.first { it.exerciseId == "ex-1" }
        assertEquals(2, p.workSets)
        assertEquals(1800.0, p.volumeKg!!, 0.0)
        assertNull(p.pinVolume)
        assertEquals(136.7, p.bestE1rm, 0.0)
        val q = d.exerciseSessions.first { it.exerciseId == "ex-2" }
        assertEquals("stack_pin", q.loadUnit)
        assertNull(q.volumeKg)
        assertEquals(96.0, q.pinVolume!!, 0.0)
    }

    @Test fun `weekly sets count machine work and ignore unfinished sessions`() {
        val d = DerivedMetrics.build(listOf(
            DerivedInput(session(1, mon), press, listOf(set(1, 100f, 10), set(2, 100f, 10))),
            DerivedInput(session(2, sat), pulldown, listOf(set(3, 8f, 12))),
            DerivedInput(session(3, sat, finished = false), press, listOf(set(4, 100f, 10)))
        ), utc)
        assertEquals(listOf("Chest" to 2, "Back" to 1), d.weeklyMuscleSets.map { it.muscle to it.workSets })
        assertEquals(1, d.exerciseBests.first { it.exerciseId == "ex-1" }.sessions)
    }
}
