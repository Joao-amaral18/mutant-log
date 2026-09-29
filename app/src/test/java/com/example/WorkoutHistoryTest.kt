package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class WorkoutHistoryTest {
    private fun workout(id: Long, time: Long, title: String = "Pull", day: Long? = null, warmup: Boolean = false, pr: Boolean = false): HistoryWorkout {
        val set = WorkoutSet(id = id, workoutExerciseId = id, setNumber = 1, weightKg = 77.5f, reps = 10, isPr = pr)
        val warm = set.copy(id = id + 100, setType = SetType.WARMUP, weightKg = 20f, reps = 5)
        return HistoryWorkout(WorkoutSession(id = id, title = title, programDayId = day, startedAt = time, finishedAt = time + 4_020_000, notes = "Execução sólida"), listOf(
            HistoryExercise(WorkoutExercise(id = id, workoutSessionId = id, exerciseId = 1, orderIndex = 0, notes = "Banco inclinado"), Exercise(id = 1, name = "Lat Pulldown", muscleGroup = "Back"),
                listOf(HistorySet(set, listOf(SetSegment(workoutSetId = id, segmentIndex = 0, weightKg = 50f, reps = 4)))) + if (warmup) listOf(HistorySet(warm)) else emptyList())
        ))
    }

    @Test fun `totals include segments and exclude warmups`() {
        val session = workout(1, 100, warmup = true, pr = true)
        assertEquals(975.0, session.volume, 0.001)
        assertEquals(14, session.reps)
        assertEquals(1, session.sets.size)
        assertEquals(1, session.prs)
        assertEquals(67, session.minutes)
        assertEquals(77.5 * (1 + 10 / 30.0), session.exercises.single().e1rm, 0.001)
    }

    @Test fun `recorded workout titles define exact filters without example categories`() {
        val first = workout(1, 100, title = "Chest + Biceps")
        val recent = workout(2, 200, title = "Chest + Biceps (leve)")
        val duplicate = workout(3, 300, title = "  chest + biceps  ")
        val custom = workout(4, 400, title = "Peito, bíceps e core")
        val sessions = listOf(first, recent, duplicate, custom)
        assertEquals(listOf("Peito, bíceps e core", "chest + biceps", "Chest + Biceps (leve)"), WorkoutHistory.titles(sessions))
        assertEquals(listOf(duplicate, first), WorkoutHistory.filter(sessions, types = setOf("Chest + Biceps")))
        assertEquals(listOf(custom), WorkoutHistory.filter(sessions, types = setOf("Peito, bíceps e core")))
        assertTrue(WorkoutHistory.filter(sessions, types = setOf("Push", "Pull", "Legs")).isEmpty())
        assertTrue(WorkoutHistory.titles(emptyList()).isEmpty())
    }

    @Test fun `search covers names notes dates decimal loads and accents across all sessions`() {
        val time = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 29, 18, 42, 0) }.timeInMillis
        val session = workout(1, time)
        listOf("pull", "pulldown", "29/09/2026", "2026-09-29", "setembro", "77,5", "77.5 kg", "execucao solida", "banco inclinado", "50").forEach { query -> assertEquals(query, listOf(session), WorkoutHistory.filter(listOf(session), query)) }
        assertTrue(WorkoutHistory.filter(listOf(session), "80").isEmpty())
    }

    @Test fun `filters combine type period and PR with local calendar boundaries`() {
        val now = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 29, 18, 0, 0); set(Calendar.MILLISECOND, 0) }
        val firstDay = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6); set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
        val first = workout(1, firstDay, pr = true)
        val earlier = workout(2, firstDay - 1, pr = true)
        val push = workout(3, now.timeInMillis, title = "Push", pr = true)
        val noPr = workout(4, now.timeInMillis)
        assertEquals(listOf(first), WorkoutHistory.filter(listOf(earlier, push, noPr, first), types = setOf("Pull"), days = 7, prsOnly = true, now = now.timeInMillis))
        assertEquals(listOf(push, noPr, first, earlier), WorkoutHistory.filter(listOf(earlier, push, noPr, first)).sortedWith(compareByDescending<HistoryWorkout> { it.session.startedAt }.thenBy { it.session.id }))
    }

    @Test fun `comparisons use earlier equivalent routine and earlier exercise regardless of workout title`() {
        val earlier = workout(1, 100, title = "Upper", day = 4)
        val differentDay = workout(2, 200, day = 5)
        val current = workout(3, 300, day = 4)
        val future = workout(4, 400, day = 4)
        assertEquals(earlier, WorkoutHistory.previous(listOf(future, current, differentDay, earlier), current))
        assertEquals(differentDay.exercises.single().sets, WorkoutHistory.previousExercise(listOf(future, current, differentDay, earlier), current, 1))
        assertNull(WorkoutHistory.previous(listOf(earlier), earlier))
    }

    @Test fun `exercise history counts sessions once and excludes exercises without logged sets`() {
        val old = workout(1, 100)
        val recent = workout(2, 200)
        val duplicate = recent.copy(exercises = recent.exercises + recent.exercises.single().copy(workoutExercise = recent.exercises.single().workoutExercise.copy(id = 99)))
        val summary = WorkoutHistory.exercises(listOf(old, duplicate)).single()
        assertEquals(2, summary.performances.size)
        assertEquals(2, summary.latest.sets.size)
        assertEquals(2L, summary.latest.workout.session.id)
        assertEquals(0.0, summary.progress, 0.001)
        assertTrue(WorkoutHistory.exercises(listOf(old.copy(exercises = old.exercises.map { it.copy(sets = emptyList()) }))).isEmpty())
    }
}
