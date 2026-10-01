package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetSegment
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import java.math.BigDecimal

private fun kg(value: Float) = BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

/** Mini-sets after the main set: drops in load, or short pauses at the same load. */
@Composable
fun IntensitySegmentDialog(
    initialWeight: Float,
    initialReps: Int,
    techniqueName: String, // "Rest-pause" or "Drop set"
    onDismiss: () -> Unit,
    onSaveSegments: (List<SetSegment>) -> Unit
) {
    val drop = techniqueName == "Drop set"
    val type = if (drop) "DROP_SET" else "REST_PAUSE"
    fun next(previousWeight: Float, previousReps: Int, index: Int) = SetSegment(
        workoutSetId = 0, segmentIndex = index, type = type,
        weightKg = if (drop) (previousWeight * 0.8f / 2.5f).toInt() * 2.5f else previousWeight,
        reps = (previousReps / 2).coerceAtLeast(3),
        restSeconds = if (drop) 5 else 20
    )
    val segments = remember { mutableStateListOf(next(initialWeight, initialReps, 1)) }

    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("intensity_segment_dialog")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MutantEyebrow("MAIN SET · ${kg(initialWeight)} KG × $initialReps", color = MutantColors.Primary)
            Text(techniqueName, style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text(
                if (drop) "Strip the load and keep going without a break." else "Rack it for about 20 seconds, then squeeze out a few more reps.",
                style = MutantType.BodySmall, color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-4).dp)
            )
            segments.forEachIndexed { index, seg ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MutantColors.Background, RoundedCornerShape(16.dp))
                        .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(16.dp))
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                        .testTag("segment_$index"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("${index + 1}", style = MutantType.MonoBody.copy(fontWeight = FontWeight.Bold), color = MutantColors.Primary,
                        modifier = Modifier.width(18.dp))
                    MiniStepper("${kg(seg.weightKg)} kg", Modifier.weight(1.2f),
                        onMinus = { segments[index] = seg.copy(weightKg = (seg.weightKg - 2.5f).coerceAtLeast(0f)) },
                        onPlus = { segments[index] = seg.copy(weightKg = seg.weightKg + 2.5f) })
                    MiniStepper("${seg.reps} reps", Modifier.weight(1f),
                        onMinus = { segments[index] = seg.copy(reps = (seg.reps - 1).coerceAtLeast(1)) },
                        onPlus = { segments[index] = seg.copy(reps = seg.reps + 1) })
                    IconButton(onClick = { segments.removeAt(index) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove segment ${index + 1}", tint = MutantColors.TextMetadata,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
            MutantButton(
                "Add segment",
                onClick = {
                    val last = segments.lastOrNull()
                    segments.add(next(last?.weightKg ?: initialWeight, last?.reps ?: initialReps, segments.size + 1))
                },
                style = MutantButtonStyle.Surface, icon = Icons.Rounded.Add, height = 48.dp, textStyle = MutantType.ButtonSmall,
                modifier = Modifier.fillMaxWidth().testTag("add_segment")
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutantButton("Cancel", onClick = onDismiss, style = MutantButtonStyle.Outline, height = 54.dp,
                    textStyle = MutantType.Button.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                MutantButton(
                    "Save ${segments.size} segment${if (segments.size == 1) "" else "s"}",
                    onClick = { onSaveSegments(segments.mapIndexed { i, s -> s.copy(segmentIndex = i + 1) }) },
                    height = 54.dp, modifier = Modifier.weight(2f).testTag("save_segments")
                )
            }
        }
    }
}

@Composable
private fun MiniStepper(value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Remove, contentDescription = "Less", tint = MutantColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
        Text(value, style = MutantType.MonoBody.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = MutantColors.TextPrimary,
            maxLines = 1, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        IconButton(onClick = onPlus, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Add, contentDescription = "More", tint = MutantColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
