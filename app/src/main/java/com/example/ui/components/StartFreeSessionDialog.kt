package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*

/** Ad-hoc workout: a name and the exercises to start with, in tap order. */
@Composable
fun StartFreeSessionDialog(
    exercises: List<Exercise>,
    isStarting: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onStart: (title: String, exerciseIds: List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("Free session") }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<List<Long>>(emptyList()) }
    val byId = remember(exercises) { exercises.associateBy { it.id } }
    val filtered = remember(exercises, query) {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        exercises.filter { ex -> terms.all { it in "${ex.name} ${ex.baseName} ${ex.muscleGroup} ${ex.manufacturer}".lowercase() } }.take(60)
    }

    MutantBottomSheet(onDismiss = onDismiss, dismissible = !isStarting, modifier = Modifier.testTag("start_free_session_dialog")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Free session", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("Not tied to your program. Pick exercises in the order you'll do them.", style = MutantType.BodySmall,
                color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-4).dp))
            MutantTextField("NAME", title, { title = it }, enabled = !isStarting, testTag = "free_session_title_input")
            if (selected.isNotEmpty()) {
                MutantEyebrow("ORDER", modifier = Modifier.padding(start = 4.dp))
                Text(selected.mapIndexedNotNull { i, id -> byId[id]?.let { "${i + 1}. ${it.baseName.ifBlank { it.name }}" } }.joinToString("  ·  "),
                    style = MutantType.BodySmall, color = MutantColors.TextPrimary)
            }
            MutantTextField("SEARCH", query, { query = it }, placeholder = "Exercise, muscle or machine", testTag = "free_session_search")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                filtered.forEach { ex ->
                    val position = selected.indexOf(ex.id)
                    val on = position >= 0
                    Surface(
                        onClick = { selected = if (on) selected - ex.id else selected + ex.id },
                        enabled = !isStarting,
                        shape = RoundedCornerShape(16.dp),
                        color = if (on) MutantColors.PrimarySelected else MutantColors.Background,
                        border = BorderStroke(1.dp, if (on) MutantColors.Primary else MutantColors.OutlineVariant),
                        modifier = Modifier.fillMaxWidth().testTag("free_session_exercise_${ex.id}")
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(ex.baseName.ifBlank { ex.name }, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                                Text(listOf(ex.muscleGroup, ex.manufacturer).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MutantType.Caption, color = MutantColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (on) Text("${position + 1}", style = MutantType.MonoBody.copy(fontWeight = FontWeight.Bold), color = MutantColors.Primary)
                            Icon(if (on) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, contentDescription = null,
                                tint = if (on) MutantColors.Primary else MutantColors.TextMetadata, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                if (filtered.isEmpty()) Text("No match in the library.", style = MutantType.BodySmall, color = MutantColors.TextMetadata,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
            }
            if (error != null) Text(error, style = MutantType.BodySmall, color = MutantColors.Error)
            MutantButton(
                when {
                    isStarting -> "Starting…"
                    selected.isEmpty() -> "Pick at least one exercise"
                    else -> "Start with ${selected.size} exercise${if (selected.size == 1) "" else "s"}"
                },
                onClick = { onStart(title.trim().ifBlank { "Free session" }, selected) },
                enabled = selected.isNotEmpty(), loading = isStarting, icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.fillMaxWidth().testTag("confirm_start_free_session")
            )
        }
    }
}
