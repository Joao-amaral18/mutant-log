package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProgramDay
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Logs a workout done earlier: the user picks the day, when it started and how long it took, then fills the sets in
 * the normal session screen. Set times are spread over that window when the workout is finished.
 */
@Composable
fun PastWorkoutSheet(
    trainingDays: List<ProgramDay>,
    defaultDayId: Long?,
    isStarting: Boolean,
    onConfirm: (day: ProgramDay, startedAt: Long, durationMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
    now: Long = System.currentTimeMillis()
) {
    var dayId by remember { mutableStateOf(defaultDayId ?: trainingDays.firstOrNull()?.id) }
    var daysAgo by remember { mutableIntStateOf(0) }
    var hour by remember { mutableIntStateOf(Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY).let { (it - 1).coerceAtLeast(0) }) }
    var minute by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(60) }
    val startedAt = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val inFuture = startedAt + duration * 60_000L > now
    val day = trainingDays.firstOrNull { it.id == dayId }

    MutantBottomSheet(onDismiss = onDismiss, dismissible = !isStarting, modifier = Modifier.testTag("past_workout_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Log a past workout", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("Enter when it happened. You'll log the sets next; rest times and duration come from these values.",
                style = MutantType.BodySmall, color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-6).dp))

            MutantEyebrow("WORKOUT", modifier = Modifier.padding(start = 4.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                trainingDays.forEach { d ->
                    MutantChoiceChip(d.title, d.id == dayId, { dayId = d.id }, modifier = Modifier.testTag("past_day_${d.id}"))
                }
            }

            MutantEyebrow("DATE", modifier = Modifier.padding(start = 4.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..6).forEach { ago ->
                    val label = when (ago) {
                        0 -> "Today"
                        1 -> "Yesterday"
                        else -> SimpleDateFormat("EEE d", Locale.US).format(Date(now - ago * 86_400_000L))
                    }
                    MutantChoiceChip(label, ago == daysAgo, { daysAgo = ago }, modifier = Modifier.testTag("past_date_$ago"))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutantStepper(
                    label = "START HOUR", value = "%02d".format(hour), onValueChange = { v -> v.toIntOrNull()?.let { hour = it.coerceIn(0, 23) } },
                    onMinus = { hour = (hour + 23) % 24 }, onPlus = { hour = (hour + 1) % 24 },
                    tag = "past_hour", allowDecimal = false, modifier = Modifier.weight(1f)
                )
                MutantStepper(
                    label = "MINUTE", value = "%02d".format(minute), onValueChange = { v -> v.toIntOrNull()?.let { minute = it.coerceIn(0, 59) } },
                    onMinus = { minute = (minute + 45) % 60 }, onPlus = { minute = (minute + 15) % 60 },
                    tag = "past_minute", allowDecimal = false, modifier = Modifier.weight(1f)
                )
            }
            MutantStepper(
                label = "DURATION · MIN", value = "$duration", onValueChange = { v -> v.toIntOrNull()?.let { duration = it.coerceIn(1, 600) } },
                onMinus = { duration = (duration - 5).coerceAtLeast(5) }, onPlus = { duration = (duration + 5).coerceAtMost(600) },
                tag = "past_duration", allowDecimal = false, modifier = Modifier.fillMaxWidth()
            )
            if (inFuture) Text("That would end in the future. Pick an earlier start.", style = MutantType.Caption,
                color = MutantColors.Error, modifier = Modifier.testTag("past_future_error"))

            MutantButton(
                text = if (isStarting) "Opening…" else "Log sets",
                onClick = { day?.let { onConfirm(it, startedAt, duration) } },
                enabled = day != null && !inFuture && !isStarting,
                loading = isStarting,
                modifier = Modifier.fillMaxWidth().testTag("past_confirm")
            )
        }
    }
}
