package com.example.data.model

import java.util.Calendar
import java.util.TimeZone

enum class RecommendationBasis {
    HISTORY,
    PERSISTED_POSITION,
    LOCAL_CALENDAR,
    UNAVAILABLE
}

data class WorkoutRecommendation(
    val programDayId: Long? = null,
    val title: String = "Nenhum treino configurado",
    val dayCode: String = "",
    val nextPosition: Int = -1,
    val totalTrainingDays: Int = 0,
    val lastWorkoutTitle: String? = null,
    val lastWorkoutRelativeDate: String? = null,
    val daysSinceLastWorkout: Int? = null,
    val basis: RecommendationBasis = RecommendationBasis.UNAVAILABLE
) {
    val lastWorkoutSummary: String
        get() = if (lastWorkoutTitle == null || lastWorkoutRelativeDate == null) {
            "nenhum treino concluído"
        } else {
            "$lastWorkoutTitle · $lastWorkoutRelativeDate"
        }
}

/**
 * Computes the next workout entirely from local data and the device clock.
 *
 * A completed session that belongs to the active routine always wins over the
 * weekday. The local weekday is only the first-run fallback, before the user
 * has any real routine history.
 */
object WorkoutRecommendationEngine {
    private const val MILLIS_PER_DAY = 86_400_000L

    fun recommend(
        programDays: List<ProgramDay>,
        lastWorkout: WorkoutSession?,
        persistedPosition: Int = -1,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): WorkoutRecommendation {
        val trainingDays = programDays
            .filterNot { it.isRestDay }
            .sortedWith(compareBy<ProgramDay> { it.dayIndex }.thenBy { it.id })

        if (trainingDays.isEmpty()) return WorkoutRecommendation()

        val historicalPosition = lastWorkout
            ?.programDayId
            ?.let { completedDayId -> trainingDays.indexOfFirst { it.id == completedDayId } }
            ?: -1

        val (nextPosition, basis) = when {
            historicalPosition >= 0 -> ((historicalPosition + 1) % trainingDays.size) to RecommendationBasis.HISTORY
            persistedPosition in trainingDays.indices -> ((persistedPosition + 1) % trainingDays.size) to RecommendationBasis.PERSISTED_POSITION
            else -> calendarFallbackPosition(trainingDays, nowMillis, timeZone) to RecommendationBasis.LOCAL_CALENDAR
        }

        val nextDay = trainingDays[nextPosition]
        val completedAt = lastWorkout?.finishedAt ?: lastWorkout?.startedAt
        val daysSince = completedAt?.let { localDaysBetween(it, nowMillis, timeZone) }

        return WorkoutRecommendation(
            programDayId = nextDay.id,
            title = nextDay.title,
            dayCode = nextDay.dayCode,
            nextPosition = nextPosition,
            totalTrainingDays = trainingDays.size,
            lastWorkoutTitle = lastWorkout?.title,
            lastWorkoutRelativeDate = daysSince?.let(::relativeDayLabel),
            daysSinceLastWorkout = daysSince,
            basis = basis
        )
    }

    fun localDaysBetween(thenMillis: Long, nowMillis: Long, timeZone: TimeZone): Int {
        val thenDay = localEpochDay(thenMillis, timeZone)
        val nowDay = localEpochDay(nowMillis, timeZone)
        return (nowDay - thenDay).coerceAtLeast(0).toInt()
    }

    private fun relativeDayLabel(daysAgo: Int): String = when (daysAgo) {
        0 -> "hoje"
        1 -> "ontem"
        else -> "há $daysAgo dias"
    }

    private fun calendarFallbackPosition(
        trainingDays: List<ProgramDay>,
        nowMillis: Long,
        timeZone: TimeZone
    ): Int {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val todayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

        return trainingDays.indices.minByOrNull { position ->
            val configuredDay = Math.floorMod(trainingDays[position].dayIndex, 7)
            Math.floorMod(configuredDay - todayIndex, 7)
        } ?: 0
    }

    private fun localEpochDay(timestamp: Long, timeZone: TimeZone): Long {
        val localWallClockMillis = timestamp + timeZone.getOffset(timestamp)
        return Math.floorDiv(localWallClockMillis, MILLIS_PER_DAY)
    }
}
