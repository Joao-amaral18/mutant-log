package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.designsystem.*
import com.example.ui.designsystem.components.MutantCard

@Composable
internal fun HistoryExerciseList(summaries: List<HistoryExerciseSummary>, onOpen: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(HistoryExerciseSort.RECENT) }
    var menu by remember { mutableStateOf(false) }
    val matching = remember(summaries, query, sort) {
        val filtered = summaries.filter { WorkoutHistory.normalize(query) in WorkoutHistory.normalize(it.exercise.name) }
        when (sort) {
            HistoryExerciseSort.RECENT -> filtered.sortedByDescending { it.latest.workout.session.startedAt }
            HistoryExerciseSort.NAME -> filtered.sortedBy { WorkoutHistory.normalize(it.exercise.name) }
            HistoryExerciseSort.USED -> filtered.sortedByDescending { it.performances.size }
            HistoryExerciseSort.PROGRESS -> filtered.sortedByDescending { it.progress }
        }
    }
    LazyColumn(contentPadding = PaddingValues(MutantSpacing.md), verticalArrangement = Arrangement.spacedBy(MutantSpacing.sm), modifier = Modifier.testTag("history_exercises")) {
        item { HistorySearch(query, { query = it }, "Search exercises…", "history_exercise_search") }
        item {
            Box {
                OutlinedButton(onClick = { menu = true }, modifier = Modifier.testTag("history_sort")) { Text(sort.label) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    HistoryExerciseSort.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option; menu = false }) }
                }
            }
        }
        if (matching.isEmpty()) item { HistoryEmpty("No exercises yet.", "Exercises from finished sessions show up here.") }
        items(matching, key = { it.exercise.id }) { summary ->
            MutantCard(onClick = { onOpen(summary.exercise.id) }, testTag = "history_exercise_${summary.exercise.id}") {
                Text(summary.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${summary.exercise.muscleGroup} · ${summary.performances.size} sessions", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
                Spacer(Modifier.height(MutantSpacing.xs))
                Text("Last: ${summary.latest.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "No work sets"}", style = MaterialTheme.typography.bodyMedium)
                Text("Best load: ${summary.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "—"}", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
                Text("e1RM: ${historyNumber(summary.e1rm)} kg", style = MaterialTheme.typography.bodySmall, color = MutantColors.Primary)
            }
        }
    }
}

@Composable
internal fun HistoryExerciseDetail(summary: HistoryExerciseSummary, onBack: () -> Unit, onSession: (Long) -> Unit) {
    var metric by rememberSaveable { mutableStateOf("e1RM") }
    var allRecords by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().testTag("history_exercise_detail")) {
        HistoryHeader(summary.exercise.name, onBack)
        LazyColumn(contentPadding = PaddingValues(MutantSpacing.md), verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)) {
            item {
                HistoryMetrics(listOf("Last performance" to (summary.latest.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "—"), "Best load" to (summary.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "—"), "Estimated e1RM" to "${historyNumber(summary.e1rm)} kg", "Sessions" to summary.performances.size.toString()))
            }
            item {
                Text("PROGRESS", style = MaterialTheme.typography.labelLarge, color = MutantColors.TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                    items(listOf("e1RM", "Load", "Volume", "Reps")) { option -> FilterChip(selected = metric == option, onClick = { metric = option }, label = { Text(option) }, modifier = Modifier.testTag("history_metric_$option")) }
                }
                val points = summary.performances.reversed().map { p -> when (metric) { "Load" -> p.bestSet?.weightKg?.toDouble() ?: 0.0; "Volume" -> p.volume; "Reps" -> p.reps.toDouble(); else -> p.e1rm } }
                val unit = if (metric == "Reps") "reps" else "kg"
                HistoryEvolutionChart(points, metric, unit)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(historyDate(summary.performances.last().workout.session.startedAt, "dd/MM/yy"), style = MaterialTheme.typography.labelSmall, color = MutantColors.TextSecondary)
                    Text(historyDate(summary.latest.workout.session.startedAt, "dd/MM/yy"), style = MaterialTheme.typography.labelSmall, color = MutantColors.TextSecondary)
                }
                if (metric == "e1RM") Text("Epley estimate: load × (1 + reps / 30). Work sets only; segments are not counted.", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = MutantSpacing.xs))
            }
            item { Text("PERSONAL RECORDS", style = MaterialTheme.typography.labelLarge, color = MutantColors.TextSecondary) }
            val records = summary.performances.flatMap { performance -> performance.sets.filter { it.set.isPr }.map { performance.workout to it } }
            if (records.isEmpty()) item { Text("No PRs yet.", color = MutantColors.TextSecondary) }
            items(if (allRecords) records else records.take(3), key = { "pr-${it.second.set.id}" }) { (workout, set) ->
                MutantCard(onClick = { onSession(workout.session.id) }) {
                    Text("${historyDate(workout.session.startedAt, "dd/MM/yyyy")} · ${workout.session.title}", style = MaterialTheme.typography.labelLarge)
                    HistorySetDetails(set)
                }
            }
            if (records.size > 3) item { TextButton(onClick = { allRecords = !allRecords }) { Text(if (allRecords) "Show fewer records" else "See all ${records.size} PRs") } }
            item { Text("SET HISTORY", style = MaterialTheme.typography.labelLarge, color = MutantColors.TextSecondary) }
            items(summary.performances, key = { "session-${it.workout.session.id}" }) { performance ->
                MutantCard(onClick = { onSession(performance.workout.session.id) }, testTag = "history_exercise_session_${performance.workout.session.id}") {
                    Text("${historyDate(performance.workout.session.startedAt, "dd MMM yyyy").uppercase(historyLocale)} · ${performance.workout.session.title} ›", fontWeight = FontWeight.Bold)
                    Text("${performance.sets.size} sets · ${historyNumber(performance.volume)} kg", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
                    performance.entries.forEach { entry ->
                        if (entry.workoutExercise.notes.isNotBlank()) Text(entry.workoutExercise.notes, color = MutantColors.TextSecondary)
                        entry.sets.forEach { set -> Spacer(Modifier.height(MutantSpacing.xs)); HistorySetDetails(set) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEvolutionChart(points: List<Double>, metric: String, unit: String) {
    val min = points.minOrNull() ?: 0.0
    val max = points.maxOrNull() ?: 0.0
    Text("${historyNumber(min)}–${historyNumber(max)} $unit · ${points.size} sessions", style = MaterialTheme.typography.labelMedium, color = MutantColors.TextSecondary)
    Canvas(Modifier.fillMaxWidth().height(180.dp).padding(vertical = MutantSpacing.sm).semantics { contentDescription = "$metric: ${points.joinToString { historyNumber(it) }} $unit, first to last session" }) {
        val inset = 6.dp.toPx()
        val width = size.width - inset * 2
        val height = size.height - inset * 2
        repeat(3) { index ->
            val y = inset + height * index / 2
            drawLine(MutantColors.OutlineVariant, Offset(inset, y), Offset(size.width - inset, y), 1.dp.toPx())
        }
        val coordinates = points.mapIndexed { index, value ->
            Offset(if (points.size == 1) size.width / 2 else inset + width * index / (points.size - 1), if (max == min) size.height / 2 else inset + height * (1 - (value - min) / (max - min)).toFloat())
        }
        val path = Path()
        coordinates.forEachIndexed { index, point -> if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y) }
        drawPath(path, MutantColors.Primary, style = Stroke(2.dp.toPx()))
        coordinates.forEach { drawCircle(MutantColors.Primary, 3.dp.toPx(), it) }
    }
}
