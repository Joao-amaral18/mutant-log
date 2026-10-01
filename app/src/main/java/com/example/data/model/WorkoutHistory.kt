package com.example.data.model

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

data class HistorySet(val set: WorkoutSet, val segments: List<SetSegment> = emptyList()) {
    val reps get() = set.reps + segments.sumOf { it.reps }
    val volume get() = set.weightKg.toDouble() * set.reps + segments.sumOf { it.weightKg.toDouble() * it.reps }
}

data class HistoryExercise(val workoutExercise: WorkoutExercise, val exercise: Exercise, val sets: List<HistorySet>) {
    val workSets get() = sets.filter { it.set.setType == SetType.WORK }
    val volume get() = workSets.sumOf { it.volume }
    val reps get() = workSets.sumOf { it.reps }
    val bestSet get() = workSets.maxWithOrNull(compareBy<HistorySet> { it.set.weightKg }.thenBy { it.set.reps })?.set
    val e1rm get() = workSets.maxOfOrNull { it.set.weightKg.toDouble() * (1 + it.set.reps / 30.0) } ?: 0.0
}

data class HistoryWorkout(val session: WorkoutSession, val exercises: List<HistoryExercise>, val cardio: List<CardioSession> = emptyList()) {
    val sets get() = exercises.flatMap { it.workSets }
    val volume get() = exercises.sumOf { it.volume }
    val reps get() = exercises.sumOf { it.reps }
    val prs get() = sets.count { it.set.isPr }
    val minutes get() = ((requireNotNull(session.finishedAt) - session.startedAt).coerceAtLeast(0) / 60_000).toInt()
}

data class ExercisePerformance(val workout: HistoryWorkout, val entries: List<HistoryExercise>) {
    val sets get() = entries.flatMap { it.workSets }
    val volume get() = entries.sumOf { it.volume }
    val reps get() = entries.sumOf { it.reps }
    val e1rm get() = entries.maxOfOrNull { it.e1rm } ?: 0.0
    val bestSet get() = sets.maxWithOrNull(compareBy<HistorySet> { it.set.weightKg }.thenBy { it.set.reps })?.set
}

data class HistoryExerciseSummary(val exercise: Exercise, val performances: List<ExercisePerformance>) {
    val latest get() = performances.first()
    val bestSet get() = performances.mapNotNull { it.bestSet }.maxWithOrNull(compareBy<WorkoutSet> { it.weightKg }.thenBy { it.reps })
    val e1rm get() = performances.maxOfOrNull { it.e1rm } ?: 0.0
    val progress get(): Double {
        val first = performances.last().e1rm
        return if (first > 0) (latest.e1rm - first) / first * 100 else 0.0
    }
}

enum class HistoryExerciseSort(val label: String) { RECENT("Most recent"), NAME("Name"), USED("Most used"), PROGRESS("Most progress") }

object WorkoutHistory {
    fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "")

    fun titles(workouts: List<HistoryWorkout>): List<String> = filter(workouts)
        .map { it.session.title.trim() }.filter { it.isNotEmpty() }.distinctBy(::normalize)

    fun filter(workouts: List<HistoryWorkout>, query: String = "", types: Set<String> = emptySet(), days: Int? = null, prsOnly: Boolean = false, now: Long = System.currentTimeMillis()): List<HistoryWorkout> {
        val cutoff = days?.let { Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_YEAR, -(it - 1)) }.timeInMillis }
        val terms = normalize(query.trim()).split("\\s+".toRegex()).filter { it.isNotEmpty() }
        return workouts.filter { workout ->
            (cutoff == null || workout.session.startedAt >= cutoff) && (!prsOnly || workout.prs > 0) &&
                (types.isEmpty() || types.any { normalize(it.trim()) == normalize(workout.session.title.trim()) }) &&
                (terms.isEmpty() || run {
                    val text = normalize(buildString {
                        append(workout.session.title); append(' '); append(workout.session.notes)
                        listOf("dd/MM/yyyy", "yyyy-MM-dd", "d MMMM yyyy").forEach { append(' '); append(SimpleDateFormat(it, Locale.forLanguageTag("pt-BR")).format(Date(workout.session.startedAt))) }
                        workout.exercises.forEach { ex ->
                            append(' '); append(ex.exercise.name); append(' '); append(ex.workoutExercise.notes)
                            ex.sets.forEach { s -> (listOf(s.set.weightKg) + s.segments.map { it.weightKg }).forEach { append(' '); append(it.toString()); append(" kg "); append(it.toString().replace('.', ',')); append(" kg") } }
                        }
                        workout.cardio.forEach { append(' '); append(it.notes); append(' '); append(it.machine) }
                    })
                    terms.all { it in text }
                })
        }.sortedWith(compareByDescending<HistoryWorkout> { it.session.startedAt }.thenByDescending { it.session.id })
    }

    fun exercises(workouts: List<HistoryWorkout>, sort: HistoryExerciseSort = HistoryExerciseSort.RECENT): List<HistoryExerciseSummary> {
        val ordered = filter(workouts)
        val ids = ordered.flatMap { it.exercises }.map { it.exercise.id }.distinct()
        val summaries = ids.map { id ->
            val performances = ordered.mapNotNull { workout ->
                val entries = workout.exercises.filter { it.exercise.id == id && it.sets.isNotEmpty() }
                if (entries.isEmpty()) null else ExercisePerformance(workout, entries)
            }
            performances.takeIf { it.isNotEmpty() }?.let { HistoryExerciseSummary(it.first().entries.first().exercise, it) }
        }.filterNotNull()
        return when (sort) {
            HistoryExerciseSort.RECENT -> summaries.sortedByDescending { it.latest.workout.session.startedAt }
            HistoryExerciseSort.NAME -> summaries.sortedBy { normalize(it.exercise.name) }
            HistoryExerciseSort.USED -> summaries.sortedByDescending { it.performances.size }
            HistoryExerciseSort.PROGRESS -> summaries.sortedByDescending { it.progress }
        }
    }

    fun previous(workouts: List<HistoryWorkout>, current: HistoryWorkout): HistoryWorkout? = filter(workouts).firstOrNull {
        it.session.startedAt < current.session.startedAt && if (current.session.programDayId != null) it.session.programDayId == current.session.programDayId else normalize(it.session.title.trim()) == normalize(current.session.title.trim())
    }

    fun previousExercise(workouts: List<HistoryWorkout>, current: HistoryWorkout, exerciseId: Long): List<HistorySet> = filter(workouts).firstOrNull {
        it.session.startedAt < current.session.startedAt && it.exercises.any { ex -> ex.exercise.id == exerciseId && ex.sets.isNotEmpty() }
    }?.exercises?.filter { it.exercise.id == exerciseId }?.flatMap { it.sets }?.sortedBy { it.set.setNumber }.orEmpty()
}
