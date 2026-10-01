package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HistoryWorkout
import com.example.ui.designsystem.*
import com.example.ui.designsystem.components.MutantEyebrow
import java.util.Calendar
import java.util.Locale

@Composable
internal fun HistoryMonthSelector(
    month: Calendar,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    trailing: String? = null
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(historyDate(month.timeInMillis, "MMMM yyyy"), style = MutantType.Title, color = MutantColors.TextPrimary,
            modifier = Modifier.weight(1f))
        if (trailing != null) MutantEyebrow(trailing, color = MutantColors.TextSecondary,
            style = MutantType.MonoLabel, modifier = Modifier.padding(end = 4.dp))
        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp).testTag("history_previous_month")) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", tint = MutantColors.TextSecondary)
        }
        IconButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.size(36.dp).testTag("history_next_month")) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month",
                tint = if (nextEnabled) MutantColors.TextSecondary else MutantColors.OutlineVariant)
        }
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
    val moveMonth: (Int) -> Unit = { delta ->
        offset += delta
        selected = historyDate(historyMonth(offset).timeInMillis, "yyyy-MM-dd")
    }
    LazyColumn(
        Modifier.fillMaxSize().testTag("history_calendar"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "month") {
            HistoryMonthSelector(month, { moveMonth(-1) }, { moveMonth(1) }, offset < 0,
                trailing = "${monthSessions.size} SESSION${if (monthSessions.size == 1) "" else "S"}")
        }
        item(key = "grid") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                        Text(it, modifier = Modifier.weight(1f).wrapContentWidth().padding(bottom = 6.dp),
                            style = MutantType.Eyebrow, color = MutantColors.TextMetadata)
                    }
                }
                (0 until (first + days + 6) / 7).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (0..6).forEach { column ->
                            val day = row * 7 + column - first + 1
                            if (day !in 1..days) Spacer(Modifier.weight(1f).aspectRatio(1f)) else {
                                val date = "$monthKey-${day.toString().padStart(2, '0')}"
                                val count = byDate[date]?.size ?: 0
                                val isSelected = date == selected
                                val isToday = date == today
                                Surface(
                                    onClick = { selected = date },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .testTag("history_day_$date")
                                        .semantics {
                                            contentDescription = "$day ${historyDate(month.timeInMillis, "MMMM yyyy")}, $count workouts" +
                                                (if (isToday) ", today" else "") + (if (isSelected) ", selected" else "")
                                        },
                                    shape = MutantShapeTokens.InputChip,
                                    color = when {
                                        isSelected -> MutantColors.PrimarySelected
                                        count > 0 -> MutantColors.SurfaceContainer
                                        else -> Color.Transparent
                                    },
                                    border = when {
                                        isSelected -> BorderStroke(1.dp, MutantColors.Primary)
                                        isToday -> BorderStroke(1.dp, MutantColors.TextSecondary)
                                        else -> null
                                    }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
                                        Text(day.toString(), style = MutantType.MonoBody.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                            color = if (count > 0 || isToday) MutantColors.TextPrimary else MutantColors.TextMetadata)
                                        Box(Modifier.size(5.dp).background(if (count > 0) MutantColors.Primary else Color.Transparent, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "selected") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MutantColors.SurfaceContainer, RoundedCornerShape(18.dp))
                    .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp)
                    .testTag("history_selected_day"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MutantEyebrow(historyDate(SimpleHistoryDate.parse(selected), "EEE MMM d").uppercase(Locale.US),
                    style = MutantType.Eyebrow.copy(fontSize = 10.5.sp))
                Text(
                    when {
                        selectedSessions.isEmpty() -> if (selected > today) "Nothing logged yet" else "Rest day"
                        else -> selectedSessions.joinToString(" + ") { it.session.title }
                    },
                    style = MutantType.Title.copy(lineHeight = 21.sp), color = MutantColors.TextPrimary
                )
            }
        }
        items(selectedSessions, key = { it.session.id }) { workout -> HistorySessionCard(workout) { onOpen(workout.session.id) } }
    }
}

private object SimpleHistoryDate {
    fun parse(value: String): Long = java.text.SimpleDateFormat("yyyy-MM-dd", historyLocale).parse(value)!!.time
}
