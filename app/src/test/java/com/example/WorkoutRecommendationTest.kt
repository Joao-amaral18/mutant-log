package com.example

import com.example.data.model.ProgramDay
import com.example.data.model.RecommendationBasis
import com.example.data.model.WorkoutRecommendationEngine
import com.example.data.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class WorkoutRecommendationTest {
    private val timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
    private val days = listOf(
        ProgramDay(id = 1, programId = 10, dayIndex = 0, dayCode = "SEG", title = "Push"),
        ProgramDay(id = 2, programId = 10, dayIndex = 1, dayCode = "TER", title = "Pull"),
        ProgramDay(id = 3, programId = 10, dayIndex = 2, dayCode = "QUA", title = "Rest", isRestDay = true),
        ProgramDay(id = 4, programId = 10, dayIndex = 3, dayCode = "QUI", title = "Legs")
    )

    @Test
    fun `real history wins over the current weekday`() {
        val now = localTime(2026, Calendar.OCTOBER, 1, 12, 0) // Thursday
        val last = WorkoutSession(
            id = 20,
            programDayId = 1,
            title = "Push",
            startedAt = now - 3_600_000,
            finishedAt = now - 1_800_000
        )

        val result = WorkoutRecommendationEngine.recommend(days, last, nowMillis = now, timeZone = timeZone)

        assertEquals("Pull", result.title)
        assertEquals(RecommendationBasis.HISTORY, result.basis)
        assertEquals("Push · hoje", result.lastWorkoutSummary)
    }

    @Test
    fun `sequence wraps and skips rest days`() {
        val now = localTime(2026, Calendar.OCTOBER, 2, 12, 0)
        val last = WorkoutSession(id = 21, programDayId = 4, title = "Legs", finishedAt = now - 1_000)

        val result = WorkoutRecommendationEngine.recommend(days, last, nowMillis = now, timeZone = timeZone)

        assertEquals("Push", result.title)
        assertEquals(0, result.nextPosition)
        assertEquals(3, result.totalTrainingDays)
    }

    @Test
    fun `local weekday is only the first-run fallback`() {
        val wednesday = localTime(2026, Calendar.SEPTEMBER, 30, 8, 0)

        val result = WorkoutRecommendationEngine.recommend(days, null, nowMillis = wednesday, timeZone = timeZone)

        assertEquals("Legs", result.title)
        assertEquals(RecommendationBasis.LOCAL_CALENDAR, result.basis)
    }

    @Test
    fun `relative date follows local calendar boundaries`() {
        val finished = localTime(2026, Calendar.SEPTEMBER, 28, 23, 50)
        val now = localTime(2026, Calendar.SEPTEMBER, 29, 0, 10)
        val last = WorkoutSession(id = 22, programDayId = 1, title = "Push", finishedAt = finished)

        val result = WorkoutRecommendationEngine.recommend(days, last, nowMillis = now, timeZone = timeZone)

        assertEquals("Push · ontem", result.lastWorkoutSummary)
        assertEquals(1, result.daysSinceLastWorkout)
    }

    private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        GregorianCalendar(timeZone).apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }.timeInMillis
}
