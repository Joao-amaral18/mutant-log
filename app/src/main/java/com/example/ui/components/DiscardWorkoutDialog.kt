package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.designsystem.MutantShapeTokens

@Composable
fun DiscardWorkoutDialog(isDiscarding: Boolean, error: String?, onDismiss: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = { if (!isDiscarding) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !isDiscarding, dismissOnClickOutside = !isDiscarding)) {
        Surface(modifier = Modifier.testTag("discard_confirmation"), shape = MutantShapeTokens.LargePanel, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp))
                }
                Text(if (isDiscarding) "Discarding workout..." else "Discard workout?",
                    style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                if (isDiscarding) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                } else {
                    Text("This workout and all sets logged during this session will be deleted.",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Cancel") }
                        Button(onClick = onDiscard,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("confirm_discard_workout")) { Text("Discard") }
                    }
                }
            }
        }
    }
}
