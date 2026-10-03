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
import com.example.ui.designsystem.components.MutantChoiceChip
import com.example.ui.designsystem.components.MutantEyebrow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
internal fun HistoryExerciseList(summaries: List<HistoryExerciseSummary>, onOpen: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(HistoryExerciseSort.RECENT) }
    val matching = remember(summaries, query, sort) {
        val needle = WorkoutHistory.normalize(query)
        val filtered = summaries.filter { needle in WorkoutHistory.normalize(it.exercise.name) }
        when (sort) {
            HistoryExerciseSort.RECENT -> filtered.sortedByDescending { it.latest.workout.session.startedAt }
            HistoryExerciseSort.NAME -> filtered.sortedBy { WorkoutHistory.normalize(it.exercise.name) }
            HistoryExerciseSort.USED -> filtered.sortedByDescending { it.performances.size }
            HistoryExerciseSort.PROGRESS -> filtered.sortedByDescending { it.progress }
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.testTag("history_exercises")
    ) {
        item { HistorySearch(query, { query = it }, "Search exercises", "history_exercise_search") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("history_sort")) {
                items(HistoryExerciseSort.entries) { option ->
                    MutantChoiceChip(option.label, sort == option, { sort = option }, modifier = Modifier.testTag("history_sort_${option.name}"))
                }
            }
        }
        if (matching.isEmpty()) item { HistoryEmpty("No exercises yet.", "Exercises from finished sessions show up here.") }
        items(matching, key = { it.exercise.id }) { summary -> HistoryExerciseRow(summary) { onOpen(summary.exercise.id) } }
        if (matching.isNotEmpty()) item {
            Text("Right column: estimated 1RM (kg)", style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Normal),
                color = MutantColors.TextMetadata, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

@Composable
private fun HistoryExerciseRow(summary: HistoryExerciseSummary, onOpen: () -> Unit) {
    // Oldest to newest, last eight sessions.
    val points = summary.performances.take(8).reversed().map { it.e1rm }
    val peak = points.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val progress = summary.progress
    val last = summary.latest.bestSet?.let { "${historyNumber(it.weightKg)} × ${it.reps}" } ?: "No work sets"
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("history_exercise_${summary.exercise.id}")
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(summary.exercise.name, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$last · ${summary.performances.size} sessions", style = MutantType.MonoLabel.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                    color = MutantColors.TextSecondary, maxLines = 1)
            }
            Row(
                Modifier.width(56.dp).height(28.dp).semantics { contentDescription = "e1RM trend over ${points.size} sessions" },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                points.forEachIndexed { index, value ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((value / peak).toFloat().coerceIn(0.15f, 1f))
                            .background(if (index == points.lastIndex) MutantColors.Primary else MutantColors.Outline, RoundedCornerShape(1.dp))
                    )
                }
            }
            Column(Modifier.width(58.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(historyNumber(summary.e1rm), style = MutantType.MonoValue.copy(fontSize = 16.sp, lineHeight = 18.sp), color = MutantColors.TextPrimary)
                Text(
                    when {
                        summary.performances.size < 2 -> "new"
                        progress > 0 -> "+${historyNumber(progress)}%"
                        else -> "${historyNumber(progress)}%"
                    },
                    style = MutantType.MonoLabel.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
                    color = when {
                        summary.performances.size < 2 -> MutantColors.TextMetadata
                        progress > 0 -> MutantColors.Success
                        progress < 0 -> MutantColors.Warning
                        else -> MutantColors.TextMetadata
                    }
                )
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
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)) {
            item {
                HistoryMetrics(listOf("Last performance" to (summary.latest.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "—"), "Best load" to (summary.bestSet?.let { "${historyNumber(it.weightKg)} kg × ${it.reps}" } ?: "—"), "Estimated e1RM" to "${historyNumber(summary.e1rm)} kg", "Sessions" to summary.performances.size.toString()))
            }
            item {
                MutantEyebrow("PROGRESS", modifier = Modifier.padding(bottom = 8.dp), style = MutantType.Eyebrow.copy(fontSize = 10.5.sp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("e1RM", "Load", "Volume", "Reps")) { option ->
                        MutantChoiceChip(option, metric == option, { metric = option }, modifier = Modifier.testTag("history_metric_$option"))
                    }
                }
                val points = summary.performances.reversed().map { p -> when (metric) { "Load" -> p.bestSet?.weightKg?.toDouble() ?: 0.0; "Volume" -> p.volume; "Reps" -> p.reps.toDouble(); else -> p.e1rm } }
                val unit = if (metric == "Reps") "reps" else "kg"
                HistoryEvolutionChart(points, metric, unit)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(historyDate(summary.performances.last().workout.session.startedAt, "MMM d"), style = MutantType.MonoLabel, color = MutantColors.TextSecondary)
                    Text(historyDate(summary.latest.workout.session.startedAt, "MMM d"), style = MutantType.MonoLabel, color = MutantColors.TextSecondary)
                }
                if (metric == "e1RM") Text("Epley estimate: load × (1 + reps / 30). Work sets only; segments are not counted.", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = MutantSpacing.xs))
            }
            item { MutantEyebrow("PERSONAL RECORDS", style = MutantType.Eyebrow.copy(fontSize = 10.5.sp)) }
            val records = summary.performances.flatMap { performance -> performance.sets.filter { it.set.isPr }.map { performance.workout to it } }
            if (records.isEmpty()) item { Text("No PRs yet.", style = MutantType.BodySmall, color = MutantColors.TextSecondary) }
            items(if (allRecords) records else records.take(3), key = { "pr-${it.second.set.id}" }) { (workout, set) ->
                MutantCard(onClick = { onSession(workout.session.id) }) {
                    Text("${historyDate(workout.session.startedAt, "EEE MMM d")} · ${workout.session.title}", style = MutantType.ButtonSmall, color = MutantColors.TextPrimary)
                    HistorySetDetails(set)
                }
            }
            if (records.size > 3) item { TextButton(onClick = { allRecords = !allRecords }) { Text(if (allRecords) "Show fewer records" else "See all ${records.size} PRs", style = MutantType.ButtonSmall, color = MutantColors.Primary) } }
            item { MutantEyebrow("SET HISTORY", style = MutantType.Eyebrow.copy(fontSize = 10.5.sp)) }
            items(summary.performances, key = { "session-${it.workout.session.id}" }) { performance ->
                MutantCard(onClick = { onSession(performance.workout.session.id) }, testTag = "history_exercise_session_${performance.workout.session.id}") {
                    Text("${performance.workout.session.title} ›", style = MutantType.Title, color = MutantColors.TextPrimary)
                    Text("${historyDate(performance.workout.session.startedAt, "EEE MMM d, yyyy")} · ${performance.sets.size} sets · ${historyNumber(performance.volume)} kg",
                        style = MutantType.Caption, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
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
    Text("${historyNumber(min)}–${historyNumber(max)} $unit · ${points.size} sessions", style = MutantType.MonoLabel, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = 10.dp))
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
