package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** A finished workout exercise with its rows, as read from Room. */
data class DerivedInput(
    val session: WorkoutSession,
    val exercise: Exercise,
    val sets: List<WorkoutSet>,
    val segmentsBySetId: Map<Long, List<SetSegment>> = emptyMap()
)

object DerivedMetrics {

    fun e1rm(weight: Double, reps: Int, rir: Int): Double = weight * (1 + (reps + rir) / 30.0)

    fun build(rows: List<DerivedInput>, timeZone: TimeZone = TimeZone.getDefault()): DerivedExport {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val finished = rows.filter { it.session.finishedAt != null }

        val sessions = finished.mapNotNull { row ->
            val work = row.sets.filter { it.setType == SetType.WORK }.sortedBy { it.setNumber }
            if (work.isEmpty()) return@mapNotNull null
            val stack = row.exercise.usesStack
            val volume = work.sumOf { s ->
                s.weightKg.toDouble() * s.reps + row.segmentsBySetId[s.id].orEmpty().sumOf { it.weightKg.toDouble() * it.reps }
            }
            val top = work.maxWith(compareBy<WorkoutSet> { it.weightKg }.thenBy { it.reps })
            DerivedExerciseSession(
                workoutId = "ws-${row.session.id}",
                exerciseId = "ex-${row.exercise.id}",
                date = dateFormat.format(java.util.Date(row.session.startedAt)),
                loadUnit = if (stack) "stack_pin" else "kg",
                workSets = work.size,
                totalReps = work.sumOf { s -> s.reps + row.segmentsBySetId[s.id].orEmpty().sumOf { it.reps } },
                topWeightKg = top.weightKg.toDouble(),
                topReps = top.reps,
                bestE1rm = work.maxOf { e1rm(it.weightKg.toDouble(), it.reps, it.rir) }.round1(),
                volumeKg = if (stack) null else volume.round1(),
                pinVolume = if (stack) volume.round1() else null
            )
        }

        val weekly = finished.flatMap { row ->
            val n = row.sets.count { it.setType == SetType.WORK }
            if (n == 0) emptyList() else listOf(Triple(isoWeek(row.session.startedAt, timeZone), row.exercise.muscleGroup, n))
        }.groupBy({ it.first to it.second }, { it.third })
            .map { (key, counts) -> DerivedWeeklyMuscleSets(week = key.first, muscle = key.second, workSets = counts.sum()) }
            .sortedWith(compareBy<DerivedWeeklyMuscleSets> { it.week }.thenByDescending { it.workSets }.thenBy { it.muscle })

        val bests = sessions.groupBy { it.exerciseId }.map { (id, list) ->
            val best = list.maxWith(compareBy<DerivedExerciseSession> { it.bestE1rm }.thenBy { it.date })
            DerivedExerciseBest(exerciseId = id, loadUnit = best.loadUnit, bestE1rm = best.bestE1rm,
                bestE1rmDate = best.date, sessions = list.map { it.workoutId }.distinct().size)
        }.sortedBy { it.exerciseId.removePrefix("ex-").toLongOrNull() ?: Long.MAX_VALUE }

        return DerivedExport(exerciseSessions = sessions, weeklyMuscleSets = weekly, exerciseBests = bests)
    }

    /** ISO-8601 week, e.g. "2026-W40", in the given zone. */
    fun isoWeek(millis: Long, timeZone: TimeZone): String {
        val cal = Calendar.getInstance(timeZone, Locale.US).apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            timeInMillis = millis
        }
        return "%d-W%02d".format(Locale.US, cal.weekYear, cal.get(Calendar.WEEK_OF_YEAR))
    }

    private fun Double.round1() = Math.round(this * 10) / 10.0
}
