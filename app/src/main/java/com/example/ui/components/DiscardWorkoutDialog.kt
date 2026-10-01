package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.MutantBottomSheet
import com.example.ui.designsystem.components.MutantButton
import com.example.ui.designsystem.components.MutantButtonStyle

@Composable
fun DiscardWorkoutDialog(
    isDiscarding: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    loggedSets: Int? = null
) {
    MutantBottomSheet(onDismiss = onDismiss, dismissible = !isDiscarding, modifier = Modifier.testTag("discard_confirmation")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (isDiscarding) "Discarding session…" else "Discard session?", style = MutantType.SheetTitle,
                color = MutantColors.TextPrimary)
            if (isDiscarding) {
                Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = MutantColors.Error, strokeWidth = 3.dp)
                }
            } else {
                val setsText = when (loggedSets) {
                    null -> "All sets logged in this session"
                    1 -> "1 logged set"
                    else -> "$loggedSets logged sets"
                }
                Text("$setsText will be deleted. This can’t be undone.",
                    style = MutantType.Body.copy(lineHeight = 20.sp), color = MutantColors.TextSecondary)
                error?.let { Text(it, style = MutantType.BodySmall, color = MutantColors.Error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MutantButton("Keep going", onClick = onDismiss, style = MutantButtonStyle.Outline, height = 54.dp,
                        textStyle = MutantType.Button.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                    MutantButton("Discard", onClick = onDiscard, style = MutantButtonStyle.Danger, height = 54.dp,
                        textStyle = MutantType.Button.copy(fontSize = 15.sp),
                        modifier = Modifier.weight(1f).testTag("confirm_discard_workout"))
                }
            }
        }
    }
}
