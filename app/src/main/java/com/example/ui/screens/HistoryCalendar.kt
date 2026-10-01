package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.data.model.HistoryWorkout
import com.example.ui.designsystem.*
import java.util.Calendar

@Composable
internal fun HistoryMonthSelector(month: Calendar, onPrevious: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious, modifier = Modifier.testTag("history_previous_month")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
        Text(historyDate(month.timeInMillis, "MMMM yyyy").replaceFirstChar { it.titlecase(historyLocale) }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.testTag("history_next_month")) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
    }
}

@Composable
internal fun HistoryCalendar(workouts: List<HistoryWorkout>, onOpen: (Long) -> Unit) {
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf(historyDate(System.currentTimeMillis(), "yyyy-MM-dd")) }
    val month = historyMonth(offset)
    val monthKey = historyDate(month.timeInMillis, "yyyy-MM")
    val monthSessions = workouts.filter { historyDate(it.session.startedAt, "yyyy-MM") == monthKey }
    val byDate = monthSessions.groupBy { historyDate(it.session.startedAt, "yyyy-MM-dd") }
    val days = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val first = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val today = historyDate(System.currentTimeMillis(), "yyyy-MM-dd")
    val selectedSessions = byDate[selected].orEmpty().sortedByDescending { it.session.startedAt }
    LazyColumn(Modifier.fillMaxSize().testTag("history_calendar"), contentPadding = PaddingValues(MutantSpacing.md), verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)) {
        item {
            HistoryMonthSelector(month, { offset--; selected = historyDate(historyMonth(offset).timeInMillis, "yyyy-MM-dd") }, { offset++; selected = historyDate(historyMonth(offset).timeInMillis, "yyyy-MM-dd") }, offset < 0)
            Text("${monthSessions.size} workouts this month", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { Text(it, modifier = Modifier.weight(1f).wrapContentWidth().padding(vertical = MutantSpacing.sm), style = MaterialTheme.typography.labelMedium, color = MutantColors.TextSecondary) }
            }
            (0 until (first + days + 6) / 7).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    (0..6).forEach { column ->
                        val day = row * 7 + column - first + 1
                        if (day !in 1..days) Spacer(Modifier.weight(1f).aspectRatio(1f)) else {
                            val date = "$monthKey-${day.toString().padStart(2, '0')}"
                            val count = byDate[date]?.size ?: 0
                            Surface(onClick = { selected = date }, modifier = Modifier.weight(1f).aspectRatio(1f).padding(MutantSpacing.xxs).testTag("history_day_$date").semantics { contentDescription = "$day, ${historyDate(month.timeInMillis, "MMMM yyyy")}, $count workouts${if (date == today) ", today" else ""}${if (date == selected) ", selected" else ""}" }, shape = MutantShapeTokens.SmallControl, color = when { date == selected -> MutantColors.PrimaryContainer; count > 0 -> MutantColors.SurfaceContainerHigh; else -> MutantColors.Background }, border = if (date == today) BorderStroke(MutantStrokeWidths.Standard, MutantColors.Primary) else null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(day.toString(), style = MaterialTheme.typography.bodyMedium)
                                    if (count > 0) Text("•", color = MutantColors.Primary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            Text("• Training day · Outline: today", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
        }
        item {
            Text(historyDate(SimpleHistoryDate.parse(selected), "d 'de' MMMM 'de' yyyy"), style = MaterialTheme.typography.titleMedium)
            if (selectedSessions.isEmpty()) HistoryEmpty("No workout on this day.", "Pick a marked day to open its sessions.")
        }
        items(selectedSessions, key = { it.session.id }) { workout -> HistorySessionCard(workout) { onOpen(workout.session.id) } }
    }
}

private object SimpleHistoryDate {
    fun parse(value: String): Long = java.text.SimpleDateFormat("yyyy-MM-dd", historyLocale).parse(value)!!.time
}
