package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import java.math.BigDecimal
import java.math.RoundingMode

private fun oneDecimal(value: Float): String =
    BigDecimal(value.toDouble()).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

@Composable
fun FinishSessionSheet(
    title: String,
    elapsedMinutes: Long,
    doneSets: Int,
    totalSets: Int,
    volumeKg: Float,
    initialBodyweight: Float?,
    isFinishing: Boolean,
    onSave: (notes: String, bodyweightKg: Float) -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    var bodyweight by remember { mutableStateOf(initialBodyweight?.takeIf { it > 0f }?.let(::oneDecimal) ?: "") }
    var notes by remember { mutableStateOf("") }
    fun step(delta: Float) {
        val current = bodyweight.toFloatOrNull() ?: initialBodyweight?.takeIf { it > 0f } ?: return
        bodyweight = oneDecimal((current + delta).coerceAtLeast(0f))
    }

    MutantBottomSheet(onDismiss = onDismiss, dismissible = !isFinishing, modifier = Modifier.testTag("finish_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Finish $title", style = MutantType.SheetTitle.copy(fontSize = 26.sp, lineHeight = 27.sp), color = MutantColors.TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MutantStatWell("TIME", "$elapsedMinutes min", Modifier.weight(1f))
                MutantStatWell("SETS", "$doneSets/$totalSets", Modifier.weight(1f))
                MutantStatWell("VOLUME", "${oneDecimal(volumeKg / 1000f)} t", Modifier.weight(1f))
            }
            MutantStepper(
                label = "BODYWEIGHT · KG",
                value = bodyweight,
                onValueChange = { bodyweight = it },
                onMinus = { step(-0.1f) },
                onPlus = { step(0.1f) },
                tag = "bodyweight",
                valueDescription = "Bodyweight in kilograms",
                buttonWidth = 56.dp,
                enabled = !isFinishing
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .background(MutantColors.Background, RoundedCornerShape(16.dp))
                    .border(1.dp, MutantColors.Line, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    enabled = !isFinishing,
                    textStyle = MutantType.Body.copy(color = MutantColors.TextPrimary),
                    cursorBrush = SolidColor(MutantColors.Primary),
                    modifier = Modifier.fillMaxWidth().testTag("session_notes_input"),
                    decorationBox = { inner ->
                        if (notes.isEmpty()) Text("Session notes (optional)", style = MutantType.Body, color = MutantColors.TextMetadata)
                        inner()
                    }
                )
            }
            MutantButton(
                if (isFinishing) "Saving…" else "Save workout",
                onClick = { onSave(notes, bodyweight.toFloatOrNull() ?: 0f) },
                loading = isFinishing,
                modifier = Modifier.fillMaxWidth().testTag("confirm_finish_workout")
            )
            TextButton(
                onClick = onDiscard, enabled = !isFinishing,
                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("discard_workout")
            ) { Text("Discard session", style = MutantType.ButtonSmall, color = MutantColors.Error) }
        }
    }
}

@Composable
fun ProgressionSheet(
    targetText: String,
    repRange: String,
    targetRir: Int,
    lastTimeText: String,
    reason: String?,
    onDismiss: () -> Unit
) {
    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("progression_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MutantEyebrow("DOUBLE PROGRESSION", color = MutantColors.Primary,
                style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em))
            Text(targetText, style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text(
                "Within $repRange, add reps until every set hits the top of the range at RIR $targetRir. " +
                    "Then the load goes up and reps reset to the bottom.",
                style = MutantType.Body.copy(lineHeight = 21.sp), color = MutantColors.TextSecondary
            )
            if (!reason.isNullOrBlank()) Text(reason, style = MutantType.BodySmall, color = MutantColors.TextSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val wellValue = MutantType.MonoBody.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, lineHeight = 18.sp)
                MutantStatWell("LAST TIME", lastTimeText, Modifier.weight(1f), valueStyle = wellValue)
                MutantStatWell("ADD LOAD WHEN", "all sets ≥ top", Modifier.weight(1f), valueStyle = wellValue)
            }
            MutantButton("Got it", onClick = onDismiss, style = MutantButtonStyle.Outline, height = 52.dp,
                textStyle = MutantType.Button.copy(fontSize = 15.sp), modifier = Modifier.fillMaxWidth())
        }
    }
}
