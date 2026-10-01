package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.model.CardioSession
import com.example.data.model.WorkoutRecommendationEngine
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.MutantViewModel
import java.util.TimeZone

private data class CardioMachine(val stored: String, val label: String, val icon: ImageVector)

private val Machines = listOf(
    CardioMachine("Stairmaster", "Stairmaster", Icons.Outlined.Stairs),
    CardioMachine("Incline Walk", "Incline walk", Icons.Outlined.Hiking),
    CardioMachine("Treadmill", "Treadmill", Icons.AutoMirrored.Outlined.DirectionsRun),
    CardioMachine("Bike", "Bike", Icons.AutoMirrored.Outlined.DirectionsBike)
)

private fun machineFor(stored: String) =
    Machines.firstOrNull { it.stored.equals(stored, ignoreCase = true) } ?: CardioMachine(stored, stored, Icons.AutoMirrored.Outlined.DirectionsRun)

private fun cardioWhen(timestamp: Long, now: Long): String =
    when (val days = WorkoutRecommendationEngine.localDaysBetween(timestamp, now, TimeZone.getDefault())) {
        0 -> "Today"
        1 -> "Yesterday"
        in 2..6 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }

@Composable
fun CardioScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardioSessions by viewModel.cardioSessions.collectAsState(initial = emptyList())
    val toast = LocalMutantToast.current
    val last = cardioSessions.firstOrNull()

    // Pre-fill from the last log so a repeat session is one tap; the user's own numbers only.
    var machine by remember(last?.id) { mutableStateOf(last?.machine ?: "Stairmaster") }
    var minutes by remember(last?.id) { mutableIntStateOf(last?.durationMinutes ?: 20) }
    var level by remember(last?.id) { mutableIntStateOf(last?.level ?: 8) }
    var heartRate by remember(last?.id) { mutableIntStateOf(last?.avgHeartRate ?: 130) }
    var rpe by remember(last?.id) { mutableIntStateOf(last?.rpe ?: 6) }
    val now = System.currentTimeMillis()

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MutantColors.Background).testTag("cardio_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") { Text("Cardio", style = MutantType.ScreenTitle, color = MutantColors.TextPrimary) }
        item(key = "logger") {
            Column(Modifier.testTag("cardio_logger_card"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Machines.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { m ->
                            val selected = machine.equals(m.stored, ignoreCase = true)
                            Column(
                                Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .background(if (selected) MutantColors.PrimarySelected else Color.Transparent, RoundedCornerShape(16.dp))
                                    .border(1.dp, if (selected) MutantColors.Primary else MutantColors.Line, RoundedCornerShape(16.dp))
                                    .selectable(selected, role = Role.RadioButton) { machine = m.stored }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("cardio_machine_${m.stored}"),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                val tint = if (selected) MutantColors.TextPrimary else MutantColors.TextSecondary
                                Icon(m.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                                Text(m.label, style = MutantType.ButtonSmall, color = tint)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CardioField("MIN", minutes, { minutes = (minutes - 1).coerceAtLeast(1) }, { minutes = (minutes + 1).coerceAtMost(600) }, "minutes", Modifier.weight(1f))
                    CardioField("LEVEL", level, { level = (level - 1).coerceAtLeast(1) }, { level = (level + 1).coerceAtMost(30) }, "level", Modifier.weight(1f))
                    CardioField("AVG HR", heartRate, { heartRate = (heartRate - 2).coerceAtLeast(60) }, { heartRate = (heartRate + 2).coerceAtMost(220) }, "hr", Modifier.weight(1f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MutantEyebrow("EFFORT · RPE $rpe", modifier = Modifier.padding(start = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..10).forEach { value ->
                            MutantChoiceChip(
                                label = "$value", selected = rpe == value, onClick = { rpe = value },
                                modifier = Modifier.weight(1f).testTag("cardio_rpe_$value"),
                                height = 40.dp, cornerRadius = 10.dp, horizontalPadding = 0.dp,
                                textStyle = MutantType.MonoLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                MutantButton(
                    "Save cardio",
                    onClick = {
                        val label = "${machineFor(machine).label} · $minutes min saved"
                        viewModel.logCardio(machine, minutes, level, heartRate, rpe,
                            onSaved = { toast.show(label) }, onError = { toast.show(it) })
                    },
                    height = 56.dp,
                    modifier = Modifier.fillMaxWidth().testTag("save_cardio_button")
                )
            }
        }
        item(key = "recent_label") {
            MutantEyebrow("RECENT", modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em))
        }
        if (cardioSessions.isEmpty()) {
            item(key = "recent_empty") {
                Text("No cardio logged yet.", style = MutantType.BodySmall, color = MutantColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp))
            }
        }
        items(cardioSessions, key = { it.id }) { session -> CardioLogRow(session, cardioWhen(session.timestamp, now)) }
    }
}

@Composable
private fun CardioField(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, tag: String, modifier: Modifier) {
    Column(
        modifier
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(16.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(16.dp))
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MutantEyebrow(label, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
        Text("$value", style = MutantType.MonoValue.copy(fontSize = 26.sp, lineHeight = 28.sp), color = MutantColors.TextPrimary,
            modifier = Modifier.testTag("cardio_${tag}_value"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(Triple(Icons.Rounded.Remove, onMinus, "Decrease"), Triple(Icons.Rounded.Add, onPlus, "Increase")).forEach { (icon, action, verb) ->
                FilledIconButton(
                    onClick = action,
                    modifier = Modifier.weight(1f).height(36.dp).testTag("cardio_${tag}_${verb.lowercase()}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MutantColors.SurfaceContainerHigh,
                        contentColor = MutantColors.TextSecondary)
                ) { Icon(icon, contentDescription = "$verb ${label.lowercase()}", modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun CardioLogRow(session: CardioSession, whenLabel: String) {
    val machine = machineFor(session.machine)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("cardio_log_${session.id}")
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).background(MutantColors.SurfaceContainerHigh, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(machine.icon, contentDescription = null, tint = MutantColors.Primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${machine.label} · ${session.durationMinutes} min", style = MutantType.RowTitle.copy(fontSize = 14.sp),
                    color = MutantColors.TextPrimary)
                Text("Lvl ${session.level} · ${session.avgHeartRate} bpm · RPE ${session.rpe}",
                    style = MutantType.MonoLabel.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal), color = MutantColors.TextSecondary)
            }
            Text(whenLabel, style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Normal), color = MutantColors.TextMetadata)
        }
    }
}
