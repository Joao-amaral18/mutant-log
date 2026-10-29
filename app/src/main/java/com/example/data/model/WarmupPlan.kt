package com.example.data.model

import kotlin.math.roundToInt

/** Warm-up and preparatory loads derived from the working load. */
object WarmupPlan {
    const val WARMUP_FRACTION = 0.45f // 40-50% of the working load
    const val WARMUP_REPS = 10 // 10-12 reps
    const val PREP_REST_SECONDS = 45

    data class Prep(val fraction: Float, val reps: Int)

    // 1st: 75% for 3-5 reps. 2nd: 90% for 1-2 reps.
    val PREPS = listOf(Prep(0.75f, 4), Prep(0.90f, 2))

    /** Nearest multiple of [step]; pin-loaded stacks move in whole pins. */
    fun roundLoad(kg: Float, step: Float, stack: Boolean): Float {
        val s = if (stack) 1f else step.takeIf { it.isFinite() && it > 0f } ?: 2.5f
        return ((kg / s).roundToInt() * s).coerceAtLeast(0f)
    }

    fun warmupLoad(workKg: Float, step: Float, stack: Boolean): Float = roundLoad(workKg * WARMUP_FRACTION, step, stack)

    fun prepLoad(workKg: Float, index: Int, step: Float, stack: Boolean): Float =
        roundLoad(workKg * PREPS[index.coerceIn(0, PREPS.lastIndex)].fraction, step, stack)
}
