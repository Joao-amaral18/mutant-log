package com.example.data.model
import com.example.data.db.WorkoutExerciseDetail

/** Detached draft; changes do not affect the recorded or active session until saved. */
data class WorkoutDraft(val session: WorkoutSession, val exercises: List<WorkoutExerciseDetail>, val segments: List<SetSegment> = emptyList(), val originalUpdatedAt: Long? = session.updatedAt)
fun WorkoutSession.restSecondsAt(now: Long): Int = restDeadline?.let { ((it - now).coerceAtLeast(0L) + 999L).div(1000L).toInt() } ?: restRemainingSeconds.coerceAtLeast(0)
fun formatSessionTime(seconds: Long): String = "%02d:%02d:%02d".format(seconds / 3600, seconds / 60 % 60, seconds % 60)
