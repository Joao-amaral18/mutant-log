package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import com.example.data.model.*
import com.example.ui.designsystem.*
import com.example.ui.designsystem.components.MutantCard
import com.example.ui.designsystem.components.MutantEyebrow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HistorySessionDetail(workout: HistoryWorkout, all: List<HistoryWorkout>, onBack: () -> Unit, onEdit: (Long) -> Unit, editError: String?, editing: Boolean) {
    Column(Modifier.fillMaxSize().testTag("history_session_detail")) {
        HistoryHeader(workout.session.title, onBack) {
            TextButton(onClick = { onEdit(workout.session.id) }, enabled = !editing, modifier = Modifier.testTag("history_edit")) {
                Text(if (editing) "Opening…" else "Edit", style = MutantType.ButtonSmall, color = MutantColors.Primary)
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)) {
            item {
                MutantEyebrow(historyDate(workout.session.startedAt, "EEE MMM d, yyyy").uppercase(historyLocale), style = MutantType.Eyebrow.copy(fontSize = 10.5.sp))
                Text("${historyDate(workout.session.startedAt, "HH:mm")} → ${historyDate(requireNotNull(workout.session.finishedAt), "HH:mm")} · ${historyDuration(workout.minutes)}",
                    style = MutantType.Body, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp))
                HistoryMetrics(listOf("Sets" to workout.sets.size.toString(), "Reps" to workout.reps.toString(), "Volume" to "${historyNumber(workout.volume)} kg", "PRs" to workout.prs.toString()))
                editError?.let { Text(it, color = MutantColors.Error) }
            }
            items(workout.exercises, key = { it.workoutExercise.id }) { ex ->
                val previous = WorkoutHistory.previousExercise(all, workout, ex.exercise.id)
                MutantCard {
                    Text(ex.exercise.name, style = MutantType.Title, color = MutantColors.TextPrimary)
                    if (ex.workoutExercise.notes.isNotBlank()) Text(ex.workoutExercise.notes, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = MutantSpacing.xs))
                    Spacer(Modifier.height(MutantSpacing.sm))
                    Row(Modifier.fillMaxWidth()) {
                        MutantEyebrow("PREVIOUS", Modifier.weight(1f))
                        MutantEyebrow("THIS SESSION", Modifier.weight(1f), color = MutantColors.Primary)
                    }
                    if (previous.isEmpty()) Text("First logged session for this exercise.", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary, modifier = Modifier.padding(vertical = MutantSpacing.xs))
                    // Align warmups and work sets separately, so a new warmup cannot shift the comparison.
                    ex.sets.groupBy { it.set.setType }.forEach { (type, sets) ->
                        val before = previous.filter { it.set.setType == type }
                        sets.forEachIndexed { index, set ->
                            HorizontalDivider(Modifier.padding(vertical = MutantSpacing.xs), color = MutantColors.OutlineVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)) {
                                Column(Modifier.weight(1f)) { before.getOrNull(index)?.let { HistorySetDetails(it) } ?: Text("—", color = MutantColors.TextSecondary) }
                                Column(Modifier.weight(1f)) { HistorySetDetails(set) }
                            }
                        }
                    }
                    if (ex.sets.isEmpty()) Text("No sets logged", color = MutantColors.TextSecondary)
                }
            }
            if (workout.session.notes.isNotBlank()) item {
                MutantEyebrow("NOTES", style = MutantType.Eyebrow.copy(fontSize = 10.5.sp))
                Text(workout.session.notes, style = MutantType.Body, color = MutantColors.TextPrimary, modifier = Modifier.padding(top = 6.dp))
            }
            items(workout.cardio, key = { "cardio-${it.id}" }) { cardio ->
                MutantCard {
                    Text(cardio.machine, style = MutantType.Title, color = MutantColors.TextPrimary)
                    Text("${historyDuration(cardio.durationMinutes)} · RPE ${cardio.rpe} · ${cardio.avgHeartRate} bpm · Level ${cardio.level}")
                    if (cardio.notes.isNotBlank()) Text(cardio.notes, color = MutantColors.TextSecondary)
                }
            }
            item {
                MutantEyebrow("SUMMARY", modifier = Modifier.padding(bottom = 8.dp), style = MutantType.Eyebrow.copy(fontSize = 10.5.sp))
                HistoryMetrics(listOf("Exercises" to workout.exercises.size.toString(), "Sets" to workout.sets.size.toString(), "Reps" to workout.reps.toString(), "Volume" to "${historyNumber(workout.volume)} kg", "Duration" to historyDuration(workout.minutes), "PRs" to workout.prs.toString()))
                Text("Work totals include drop-set and rest-pause segments. Warm-ups are excluded.", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
            }
            item {
                val previous = WorkoutHistory.previous(all, workout)
                if (previous != null) {
                    MutantCard {
                        MutantEyebrow("VS. LAST ${previous.session.title.uppercase(historyLocale)}", color = MutantColors.Primary)
                        Text(historyDate(previous.session.startedAt, "EEE MMM d"), style = MutantType.Caption, color = MutantColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp))
                        HistoryComparison("Volume", "${historyNumber(workout.volume)} kg", workout.volume - previous.volume, " kg")
                        HistoryComparison("Reps", workout.reps.toString(), (workout.reps - previous.reps).toDouble())
                        HistoryComparison("Sets", workout.sets.size.toString(), (workout.sets.size - previous.sets.size).toDouble())
                        HistoryComparison("PRs", workout.prs.toString(), (workout.prs - previous.prs).toDouble())
                    }
                } else Text("Comparison appears after another session of the same workout.", color = MutantColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun HistorySetDetails(history: HistorySet) {
    val set = history.set
    Text(historySetText(set), style = MutantType.MonoBody.copy(fontWeight = FontWeight.SemiBold), color = MutantColors.TextPrimary)
    Text(if (set.setType == SetType.WARMUP) "Warm-up · RIR ${set.rir}" else "RIR ${set.rir}${if (set.rir == 0) " · Failure" else ""}", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
    if (set.isPr) Text("PR · ${when (set.prType) { "LOAD" -> "Load"; "REP" -> "Reps"; else -> "Performance" }}", color = MutantColors.Warning, style = MaterialTheme.typography.labelSmall)
    if (set.technique != IntensityTechnique.NONE) Text(when (set.technique) { IntensityTechnique.REST_PAUSE -> "Rest-pause"; IntensityTechnique.DROP_SET -> "Drop set"; IntensityTechnique.ASSISTED_REPS -> "Assisted reps"; IntensityTechnique.PARTIAL_REPS -> "Partial reps"; else -> "" }, style = MaterialTheme.typography.bodySmall, color = MutantColors.Primary)
    history.segments.forEach { Text("+ ${historyNumber(it.weightKg)} × ${it.reps} · ${it.restSeconds}s", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary) }
}

@Composable
private fun HistoryComparison(label: String, value: String, delta: Double, unit: String = "") {
    Row(Modifier.fillMaxWidth().padding(top = MutantSpacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(MutantSpacing.sm))
        Text(if (delta == 0.0) "=" else "${if (delta > 0) "+" else ""}${historyNumber(delta)}$unit", color = if (delta > 0) MutantColors.Success else MutantColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
