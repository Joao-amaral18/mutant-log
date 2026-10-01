package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.repository.ReadinessInput
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.data.model.JointDiscomfortLevels
import com.example.data.model.ReadinessBand
import com.example.data.model.readinessBand

internal data class ReadinessVerdict(val label: String, val color: Color, val icon: ImageVector, val message: String)

internal fun readinessVerdict(score: Int): ReadinessVerdict = when (readinessBand(score)) {
    ReadinessBand.READY -> ReadinessVerdict("Ready", MutantColors.Success, Icons.Rounded.Bolt,
        "Green light. Go for today’s targets.")
    ReadinessBand.MODERATE -> ReadinessVerdict("Moderate", MutantColors.Warning, Icons.Rounded.Warning,
        "Partly recovered. Hold loads and stay at the top of the RIR range.")
    ReadinessBand.HIGH_FATIGUE -> ReadinessVerdict("High fatigue", MutantColors.Error, Icons.Rounded.Error,
        "Fatigue is stacking up. Drop one set per exercise, or take a rest day.")
}

@Composable
fun ReadinessDialog(
    workoutTitle: String = "",
    initialInput: ReadinessInput = ReadinessInput(),
    onDismiss: () -> Unit,
    onConfirm: (ReadinessInput) -> Unit
) {
    var input by remember { mutableStateOf(initialInput) }
    val score = input.score
    val verdict = readinessVerdict(score)

    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("readiness_dialog")) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MutantEyebrow(
                        if (workoutTitle.isBlank()) "READINESS" else "READINESS · ${workoutTitle.uppercase()}",
                        color = MutantColors.TextSecondary,
                        style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em)
                    )
                    Text("How are you feeling?", style = MutantType.SheetTitle.copy(fontSize = 26.sp, lineHeight = 27.sp), color = MutantColors.TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$score", style = MutantType.MonoValue.copy(fontSize = 28.sp, lineHeight = 30.sp), color = verdict.color,
                        modifier = Modifier.testTag("readiness_score"))
                    Text(verdict.label, style = MutantType.Chip.copy(fontSize = 11.sp), color = verdict.color)
                }
            }

            ScaleRow("Sleep", listOf("Awful", "Poor", "OK", "Good", "Great"), input.sleep, "sleep") { input = input.copy(sleep = it) }
            ScaleRow("Energy", listOf("Empty", "Low", "OK", "High", "Maxed"), input.energy, "energy") { input = input.copy(energy = it) }
            ScaleRow("Soreness", listOf("None", "Mild", "Moderate", "Strong", "Severe"), input.soreness, "soreness") { input = input.copy(soreness = it) }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Joint pain", style = MutantType.RowTitle.copy(fontSize = 14.sp), color = MutantColors.TextPrimary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JointDiscomfortLevels.forEach { level ->
                        MutantChoiceChip(
                            label = level,
                            selected = input.jointDiscomfort.equals(level, ignoreCase = true),
                            onClick = { input = input.copy(jointDiscomfort = level) },
                            modifier = Modifier.weight(1f).testTag("readiness_joint_$level"),
                            height = 44.dp, cornerRadius = 12.dp, horizontalPadding = 0.dp,
                            textStyle = MutantType.Chip.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
            ScaleRow("Focus", listOf("None", "Low", "OK", "High", "Locked in"), input.motivation, "focus") { input = input.copy(motivation = it) }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(verdict.color.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(verdict.icon, contentDescription = null, tint = verdict.color, modifier = Modifier.size(18.dp))
                Text(verdict.message, style = MutantType.BodySmall, color = MutantColors.TextPrimary)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutantButton(
                    "Cancel", onClick = onDismiss, style = MutantButtonStyle.Outline,
                    height = 56.dp, modifier = Modifier.weight(1f)
                )
                MutantButton(
                    "Start workout", onClick = { onConfirm(input) }, height = 56.dp,
                    modifier = Modifier.weight(2f).testTag("confirm_readiness_button")
                )
            }
        }
    }
}

@Composable
private fun ScaleRow(label: String, hints: List<String>, value: Int, tag: String, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MutantType.RowTitle.copy(fontSize = 14.sp), color = MutantColors.TextPrimary)
            Text(hints.getOrElse(value - 1) { "" }, style = MutantType.Caption.copy(fontWeight = FontWeight.Medium),
                color = MutantColors.TextSecondary)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..5).forEach { option ->
                MutantChoiceChip(
                    label = option.toString(), selected = option == value, onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f).testTag("readiness_${tag}_$option"),
                    height = 44.dp, cornerRadius = 12.dp, horizontalPadding = 0.dp,
                    textStyle = MutantType.Chip.copy(fontSize = 13.sp)
                )
            }
        }
    }
}
