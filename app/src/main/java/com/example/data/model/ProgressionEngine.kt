package com.example.data.model

data class ProgressionRecommendation(
    val suggestedWeightKg: Float,
    val suggestedRepsMin: Int,
    val suggestedRepsMax: Int,
    val targetRir: Int,
    val status: ProgressionStatus,
    val reason: String,
    val previousPerformance: String,
    val historyWorkSets: List<SetSummary>
)

enum class ProgressionStatus {
    INCREASE_LOAD,
    MAINTAIN_LOAD,
    DELOAD_OR_CONSOLIDATE,
    FIRST_TIME
}

data class SetSummary(
    val weightKg: Float,
    val reps: Int,
    val rir: Int,
    val isWorkSet: Boolean,
    val technique: IntensityTechnique = IntensityTechnique.NONE
)

object ProgressionEngine {

    fun computeProgression(
        lastWorkSets: List<WorkoutSet>,
        repMin: Int = 10,
        repMax: Int = 12,
        targetRir: Int = 0,
        incrementKg: Float = 2.5f,
        lastExecutionQuality: String = "Good",
        lastTargetMuscleQuality: String = "Good",
        // Pin-loaded machines: loads are stack positions, so texts say "pin 8" instead of "8 kg".
        stack: Boolean = false
    ): ProgressionRecommendation {
        fun load(v: Float) = if (stack) "pin ${v.formatLoad()}" else "${v.formatLoad()} kg"
        val workSets = lastWorkSets.filter { it.setType == SetType.WORK }

        if (workSets.isEmpty()) {
            return ProgressionRecommendation(
                suggestedWeightKg = 0f,
                suggestedRepsMin = repMin,
                suggestedRepsMax = repMax,
                targetRir = targetRir,
                status = ProgressionStatus.FIRST_TIME,
                reason = "No previous work sets recorded. Establish working baseline.",
                previousPerformance = "First session",
                historyWorkSets = emptyList()
            )
        }

        val baseWeight = workSets.first().weightKg
        val historySummaries = workSets.map {
            SetSummary(it.weightKg, it.reps, it.rir, isWorkSet = true, technique = it.technique)
        }
        val prevPerfString = workSets.joinToString(separator = ", ") { "${load(it.weightKg)} × ${it.reps} @${it.rir}RIR" }

        val isExecutionCompromised = lastExecutionQuality.equals("Compromised", ignoreCase = true) ||
                lastExecutionQuality.equals("Bad", ignoreCase = true)
        val isTargetMuscleWeak = lastTargetMuscleQuality.equals("Weak", ignoreCase = true) ||
                lastTargetMuscleQuality.equals("None", ignoreCase = true)

        if (isExecutionCompromised || isTargetMuscleWeak) {
            return ProgressionRecommendation(
                suggestedWeightKg = baseWeight,
                suggestedRepsMin = repMin,
                suggestedRepsMax = repMax,
                targetRir = targetRir,
                status = ProgressionStatus.DELOAD_OR_CONSOLIDATE,
                reason = if (isExecutionCompromised)
                    "Execution compromised last time. Maintain ${load(baseWeight)} to rebuild strict biomechanics."
                else
                    "Target muscle stimulation was weak. Consolidate execution and mind-muscle connection at ${load(baseWeight)}.",
                previousPerformance = prevPerfString,
                historyWorkSets = historySummaries
            )
        }

        // Check if all work sets reached or exceeded repMax and respected target RIR
        val allHitTopRepRange = workSets.all { it.reps >= repMax }
        val rirRespected = workSets.all { it.rir <= targetRir + 1 }

        return if (allHitTopRepRange && rirRespected) {
            val newWeight = baseWeight + incrementKg
            ProgressionRecommendation(
                suggestedWeightKg = newWeight,
                suggestedRepsMin = repMin,
                suggestedRepsMax = repMax,
                targetRir = targetRir,
                status = ProgressionStatus.INCREASE_LOAD,
                reason = "All work sets hit top bracket (≥$repMax reps) with clean execution. Progressive overload triggered: +${incrementKg.formatLoad()}${if (stack) " pin" else " kg"}.",
                previousPerformance = prevPerfString,
                historyWorkSets = historySummaries
            )
        } else {
            val minRepsLogged = workSets.minOfOrNull { it.reps } ?: 0
            ProgressionRecommendation(
                suggestedWeightKg = baseWeight,
                suggestedRepsMin = repMin,
                suggestedRepsMax = repMax,
                targetRir = targetRir,
                status = ProgressionStatus.MAINTAIN_LOAD,
                reason = "Target bracket ($repMin–$repMax reps) in progress (lowest set: $minRepsLogged reps). Dominate ${load(baseWeight)} across all sets before increasing load.",
                previousPerformance = prevPerfString,
                historyWorkSets = historySummaries
            )
        }
    }

    private fun Float.formatLoad(): String {
        return if (this % 1f == 0f) this.toInt().toString() else "%.1f".format(this)
    }
}
