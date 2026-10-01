package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.db.ProgramDaySummary
import com.example.data.model.ProgramDay
import com.example.data.model.WorkoutSession
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

private val WeekdayCodes = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

enum class WeekDayStatus { DONE, NEXT, PLANNED, REST }

data class WeekDayRow(
    val day: ProgramDay,
    val weekdayCode: String,
    val dateOfMonth: Int,
    val status: WeekDayStatus,
    val doneMinutes: Int? = null
)

/** Monday-first week of the active program, marked from finished sessions of this calendar week. */
fun buildWeekRows(
    days: List<ProgramDay>,
    finished: List<WorkoutSession>,
    recommendedDayId: Long?,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
): List<WeekDayRow> {
    val monday = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_MONTH, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }
    val weekStart = monday.timeInMillis
    val weekEnd = Calendar.getInstance(timeZone).apply { timeInMillis = weekStart; add(Calendar.DAY_OF_MONTH, 7) }.timeInMillis
    val thisWeek = finished.filter { it.startedAt in weekStart until weekEnd }
    return days.sortedWith(compareBy<ProgramDay> { it.dayIndex }.thenBy { it.id }).map { day ->
        val index = Math.floorMod(day.dayIndex, 7)
        val date = Calendar.getInstance(timeZone).apply { timeInMillis = weekStart; add(Calendar.DAY_OF_MONTH, index) }
        val done = thisWeek.firstOrNull { it.programDayId == day.id }
        val status = when {
            day.isRestDay -> WeekDayStatus.REST
            done != null -> WeekDayStatus.DONE
            day.id == recommendedDayId -> WeekDayStatus.NEXT
            else -> WeekDayStatus.PLANNED
        }
        WeekDayRow(day, WeekdayCodes[index], date.get(Calendar.DAY_OF_MONTH), status, done?.durationMinutes)
    }
}

fun todayLabel(nowMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
    return "${WeekdayCodes[(calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7]} ${calendar.get(Calendar.DAY_OF_MONTH)}"
}

/** Work time plus planned rest, rounded to five minutes. */
fun estimatedMinutes(summary: ProgramDaySummary): Int {
    val seconds = summary.restSeconds + summary.setCount * 45
    return ((seconds / 60f / 5f).roundToInt() * 5).coerceAtLeast(5)
}

fun relativeDays(days: Int?): String = when (days) {
    null -> ""
    0 -> "today"
    1 -> "yesterday"
    else -> "$days days ago"
}

fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}

data class ActiveSessionProgress(val title: String, val elapsedSeconds: Long, val doneSets: Int, val totalSets: Int)

@Composable
fun NextWorkoutCard(
    todayLabel: String,
    title: String,
    summary: ProgramDaySummary?,
    isRecovered: Boolean,
    fatigueLabel: String,
    lastTitle: String?,
    lastDaysAgo: Int?,
    recoveryText: String,
    activeSession: ActiveSessionProgress?,
    isStarting: Boolean,
    startError: String?,
    onStart: (() -> Unit)?,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("daily_status_card"),
        shape = RoundedCornerShape(26.dp),
        color = MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MutantEyebrow("NEXT · $todayLabel", color = MutantColors.TextSecondary,
                    style = MutantType.MonoLabel.copy(letterSpacing = 0.1.em))
                val statusColor = if (isRecovered) MutantColors.Success else MutantColors.Warning
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(7.dp).background(statusColor, CircleShape))
                    Text(if (isRecovered) "Recovered" else fatigueLabel, style = MutantType.Chip, color = statusColor)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MutantType.DisplayHero, color = MutantColors.TextPrimary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("next_workout_title"))
                if (summary != null) {
                    Text("${summary.exerciseCount} exercises · ${summary.setCount} sets · ~${estimatedMinutes(summary)} min",
                        style = MutantType.Body.copy(lineHeight = 18.sp), color = MutantColors.TextSecondary)
                }
            }
            Column {
                HorizontalDivider(color = MutantColors.OutlineVariant)
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutantStat("LAST", lastTitle ?: "None yet", Modifier.weight(1f),
                        caption = relativeDays(lastDaysAgo).ifBlank { "log a session" })
                    MutantStat("RECOVERY", recoveryText, Modifier.weight(1f), caption = "since last session")
                    MutantStat("FATIGUE", fatigueLabel, Modifier.weight(1f), caption = "last check-in",
                        valueColor = if (isRecovered) MutantColors.Success else MutantColors.Warning)
                }
            }
            if (activeSession != null) {
                Column(Modifier.testTag("resume_workout_banner"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("● IN PROGRESS · ${formatClock(activeSession.elapsedSeconds)}",
                            style = MutantType.MonoLabel.copy(fontSize = 12.sp), color = MutantColors.Primary)
                        Text("${activeSession.doneSets}/${activeSession.totalSets}",
                            style = MutantType.MonoLabel.copy(fontSize = 12.sp), color = MutantColors.TextSecondary)
                    }
                    ProgressTrack(
                        fraction = if (activeSession.totalSets > 0) activeSession.doneSets / activeSession.totalSets.toFloat() else 0f,
                        height = 6.dp
                    )
                    MutantButton(
                        "Resume ${activeSession.title}", onClick = onResume,
                        trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        modifier = Modifier.fillMaxWidth().testTag("resume_workout_button")
                    )
                }
            } else if (onStart != null) {
                MutantButton(
                    if (isStarting) "Starting…" else "Start workout", onClick = onStart,
                    icon = Icons.Rounded.PlayArrow, loading = isStarting,
                    modifier = Modifier.fillMaxWidth().testTag("start_workout_button")
                )
            }
            if (startError != null) Text(startError, style = MutantType.BodySmall, color = MutantColors.Error)
        }
    }
}

@Composable
fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 3.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(MutantColors.OutlineVariant, RoundedCornerShape(height / 2))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MutantColors.Primary, RoundedCornerShape(height / 2))
        )
    }
}

@Composable
fun WeekHeader(sessions: Int, restDays: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text("This week", style = MutantType.SectionTitle, color = MutantColors.TextPrimary)
        MutantEyebrow("$sessions SESSIONS · $restDays REST", color = MutantColors.TextSecondary,
            style = MutantType.MonoLabel.copy(letterSpacing = 0.sp))
    }
}

@Composable
fun WeekDayItem(
    row: WeekDayRow,
    summary: ProgramDaySummary?,
    onOpen: () -> Unit,
    onAddExercise: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (row.status == WeekDayStatus.REST) {
        Row(
            modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("program_day_${row.day.dayIndex}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(row.weekdayCode, style = MutantType.MonoLabel, color = MutantColors.TextMetadata, modifier = Modifier.width(34.dp))
            Canvas(Modifier.weight(1f).height(1.dp)) {
                drawLine(
                    MutantColors.Line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                )
            }
            Text("Rest", style = MutantType.Caption.copy(fontWeight = FontWeight.Medium), color = MutantColors.TextMetadata)
        }
        return
    }
    val next = row.status == WeekDayStatus.NEXT
    val done = row.status == WeekDayStatus.DONE
    val accent = when {
        next -> MutantColors.Primary
        done -> MutantColors.Success
        else -> MutantColors.TextSecondary
    }
    val meta = when {
        done -> "Done" + (row.doneMinutes?.takeIf { it > 0 }?.let { " · $it min" } ?: "")
        next -> "Up next" + (summary?.let { " · ${it.exerciseCount} exercises" } ?: "")
        summary != null -> "${summary.exerciseCount} exercises · ${summary.setCount} sets"
        else -> "No exercises yet"
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (next) Modifier.background(MutantColors.SurfaceContainer, RoundedCornerShape(18.dp))
                    .border(1.dp, MutantColors.Outline, RoundedCornerShape(18.dp)) else Modifier
            )
            .padding(start = 12.dp, end = 4.dp)
            .testTag("program_day_${row.day.dayIndex}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.width(34.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(row.weekdayCode, style = MutantType.MonoLabel.copy(fontWeight = FontWeight.SemiBold), color = accent)
            Text("${row.dateOfMonth}", style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Normal), color = MutantColors.TextMetadata)
        }
        Surface(onClick = onOpen, color = androidx.compose.ui.graphics.Color.Transparent, modifier = Modifier.weight(1f)) {
            Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(row.day.title, style = MutantType.RowTitle, color = MutantColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = MutantType.Caption.copy(lineHeight = 13.sp),
                    color = if (next || done) accent else MutantColors.TextSecondary)
            }
        }
        IconButton(onClick = onAddExercise, modifier = Modifier.size(44.dp).testTag("add_exercise_${row.day.dayIndex}")) {
            Icon(Icons.Rounded.Add, contentDescription = "Add exercise to ${row.day.title}", tint = MutantColors.TextSecondary)
        }
        IconButton(onClick = onOpen, modifier = Modifier.size(44.dp)) {
            Icon(
                if (done) Icons.Rounded.CheckCircle else Icons.Rounded.PlayCircleFilled,
                contentDescription = if (done) "Open ${row.day.title} in history" else "Start ${row.day.title}",
                tint = accent
            )
        }
    }
}

@Composable
fun EmptyProgramCard(
    exerciseCount: Int,
    variantCount: Int,
    machineCount: Int,
    isAdoptingTemplate: Boolean,
    onCreateProgram: () -> Unit,
    onUseTemplate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag("empty_program_card"),
        shape = RoundedCornerShape(26.dp),
        color = MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            MutantEyebrow("FRESH INSTALL", color = MutantColors.TextSecondary, style = MutantType.MonoLabel.copy(letterSpacing = 0.1.em))
            Text("No program yet", style = MutantType.DisplayLarge, color = MutantColors.TextPrimary)
            Text(
                "The exercise and machine catalog is loaded. Your history starts empty. Build a program or load the Nick Walker template.",
                style = MutantType.Body, color = MutantColors.TextSecondary
            )
            Column {
                HorizontalDivider(color = MutantColors.OutlineVariant)
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MutantStat("EXERCISES", "$exerciseCount", Modifier.weight(1f), caption = "in library")
                    MutantStat("VARIANTS", "$variantCount", Modifier.weight(1f), caption = "machine setups")
                    MutantStat("MACHINES", "$machineCount", Modifier.weight(1f), caption = "in catalog")
                }
            }
            MutantButton("Create program", onClick = onCreateProgram, icon = Icons.Rounded.Add,
                modifier = Modifier.fillMaxWidth().testTag("create_program_button"))
            MutantButton(
                if (isAdoptingTemplate) "Loading template…" else "Use Nick Walker template",
                onClick = onUseTemplate, style = MutantButtonStyle.Surface, icon = Icons.Rounded.Bolt,
                loading = isAdoptingTemplate, height = 54.dp,
                modifier = Modifier.fillMaxWidth().testTag("use_nick_walker_template_button")
            )
        }
    }
}
