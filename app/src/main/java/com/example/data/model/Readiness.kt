package com.example.data.model

import kotlin.math.roundToInt

val JointDiscomfortLevels = listOf("None", "Mild", "Moderate", "Severe")

/** 0–100. Sleep, energy and focus carry 60%; soreness and joint pain 20% each. */
fun readinessScore(sleep: Int, energy: Int, soreness: Int, jointDiscomfort: String, focus: Int): Int {
    val joint = JointDiscomfortLevels.indexOfFirst { it.equals(jointDiscomfort, ignoreCase = true) }.coerceAtLeast(0)
    val drive = (sleep + energy + focus - 3) / 12f
    val sorenessPart = (5 - soreness) / 4f
    val joints = (3 - joint) / 3f
    return ((drive * 0.6f + sorenessPart * 0.2f + joints * 0.2f) * 100).roundToInt().coerceIn(0, 100)
}

/** Bands used by the check-in and by Home: ready, moderate, high fatigue. */
enum class ReadinessBand { READY, MODERATE, HIGH_FATIGUE }

fun readinessBand(score: Int): ReadinessBand = when {
    score >= 70 -> ReadinessBand.READY
    score >= 45 -> ReadinessBand.MODERATE
    else -> ReadinessBand.HIGH_FATIGUE
}
