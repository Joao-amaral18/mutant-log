package com.example.data.model

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

data class HistorySet(val set: WorkoutSet, val segments: List<SetSegment> = emptyList()) {
    val reps get() = set.reps + segments.sumOf { it.reps }
    val volume get() = set.weightKg.toDouble() * set.reps + segments.sumOf { it.weightKg.toDouble() * it.reps }
}

// Derived values are computed once per object: History reads them for every row on every recomposition.
data class HistoryExercise(val workoutExercise: WorkoutExercise, val exercise: Exercise, val sets: List<HistorySet>) {
    val workSets by once { sets.filter { it.set.setType == SetType.WORK } }
    // Stack-pin loads are positions, not kilograms: they would distort kg volume, so they count as zero.
    val volume by once { if (exercise.usesStack) 0.0 else workSets.sumOf { it.volume } }
    val reps by once { workSets.sumOf { it.reps } }
    val bestSet by once { workSets.maxWithOrNull(compareBy<HistorySet> { it.set.weightKg }.thenBy { it.set.reps })?.set }
    val e1rm by once { workSets.maxOfOrNull { it.set.weightKg.toDouble() * (1 + it.set.reps / 30.0) } ?: 0.0 }
}

data class HistoryWorkout(val session: WorkoutSession, val exercises: List<HistoryExercise>, val cardio: List<CardioSession> = emptyList()) {
    val sets by once { exercises.flatMap { it.workSets } }
    val volume by once { exercises.sumOf { it.volume } }
    val reps by once { exercises.sumOf { it.reps } }
    val prs by once { sets.count { it.set.isPr } }
    val minutes get() = ((requireNotNull(session.finishedAt) - session.startedAt).coerceAtLeast(0) / 60_000).toInt()

    internal val normalizedTitle by once { WorkoutHistory.normalize(session.title.trim()) }

    /** Normalized text the History search matches: built once per workout, not once per keystroke. */
    internal val searchText by once {
        WorkoutHistory.normalize(buildString {
            append(session.title); append(' '); append(session.notes)
            SearchDatePatterns.forEach { append(' '); append(SimpleDateFormat(it, SearchLocale).format(Date(session.startedAt))) }
            exercises.forEach { ex ->
                append(' '); append(ex.exercise.name); append(' '); append(ex.workoutExercise.notes)
                ex.sets.forEach { s -> (listOf(s.set.weightKg) + s.segments.map { it.weightKg }).forEach { append(' '); append(it.toString()); append(" kg "); append(it.toString().replace('.', ',')); append(" kg") } }
            }
            cardio.forEach { append(' '); append(it.notes); append(' '); append(it.machine) }
        })
    }
}

private fun <T> once(compute: () -> T) = lazy(LazyThreadSafetyMode.PUBLICATION, compute)
private val SearchDatePatterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d MMMM yyyy")
private val SearchLocale = Locale.forLanguageTag("pt-BR")
private val CombiningMarks = Regex("\\p{M}+")
private val Whitespace = Regex("\\s+")
private val RecentFirst = compareByDescending<HistoryWorkout> { it.session.startedAt }.thenByDescending { it.session.id }

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
    fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(CombiningMarks, "")

    fun titles(workouts: List<HistoryWorkout>): List<String> = filter(workouts)
        .map { it.session.title.trim() }.filter { it.isNotEmpty() }.distinctBy(::normalize)

    fun filter(workouts: List<HistoryWorkout>, query: String = "", types: Set<String> = emptySet(), days: Int? = null, prsOnly: Boolean = false, now: Long = System.currentTimeMillis()): List<HistoryWorkout> {
        val cutoff = days?.let { Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.DAY_OF_YEAR, -(it - 1)) }.timeInMillis }
        val terms = normalize(query.trim()).split(Whitespace).filter { it.isNotEmpty() }
        val wantedTypes = types.mapTo(HashSet()) { normalize(it.trim()) }
        return workouts.filter { workout ->
            (cutoff == null || workout.session.startedAt >= cutoff) && (!prsOnly || workout.prs > 0) &&
                (wantedTypes.isEmpty() || workout.normalizedTitle in wantedTypes) &&
                (terms.isEmpty() || terms.all { it in workout.searchText })
        }.sortedWith(RecentFirst)
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

    /** [sorted]: [workouts] is already newest-first (from [filter]), so a detail screen can sort once for all rows. */
    fun previous(workouts: List<HistoryWorkout>, current: HistoryWorkout, sorted: Boolean = false): HistoryWorkout? =
        (if (sorted) workouts else filter(workouts)).firstOrNull {
        it.session.startedAt < current.session.startedAt && if (current.session.programDayId != null) it.session.programDayId == current.session.programDayId else normalize(it.session.title.trim()) == normalize(current.session.title.trim())
    }

    fun previousExercise(workouts: List<HistoryWorkout>, current: HistoryWorkout, exerciseId: Long, sorted: Boolean = false): List<HistorySet> =
        (if (sorted) workouts else filter(workouts)).firstOrNull {
        it.session.startedAt < current.session.startedAt && it.exercises.any { ex -> ex.exercise.id == exerciseId && ex.sets.isNotEmpty() }
    }?.exercises?.filter { it.exercise.id == exerciseId }?.flatMap { it.sets }?.sortedBy { it.set.setNumber }.orEmpty()
}
