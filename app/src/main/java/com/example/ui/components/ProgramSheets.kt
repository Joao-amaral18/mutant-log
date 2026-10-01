package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.ExerciseVariant
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*

private val MuscleFilters = listOf("All", "Chest", "Back", "Quads", "Hamstrings", "Glutes", "Delts", "Biceps", "Triceps", "Calves")

/** Permanent addition to a program day: pick an exercise, then the machine variant. */
@Composable
fun AddProgramExerciseSheet(
    dayTitle: String,
    exercises: List<Exercise>,
    variants: List<ExerciseVariant>,
    onAdd: (exercise: Exercise, variantId: String) -> Unit,
    onCreateVariant: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf("All") }
    var chosen by remember { mutableStateOf<Exercise?>(null) }
    var variantId by remember { mutableStateOf("") }
    val matches = remember(exercises, query, muscle) {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        exercises.asSequence()
            .filter { muscle == "All" || it.muscleGroup.equals(muscle, ignoreCase = true) }
            .filter { ex -> terms.all { it in "${ex.name} ${ex.baseName} ${ex.manufacturer} ${ex.movementPattern}".lowercase() } }
            .take(40).toList()
    }

    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("add_program_exercise_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add to $dayTitle", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("Saved to your program. Future sessions of this day include it.", style = MutantType.BodySmall,
                color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-4).dp))
            val current = chosen
            if (current == null) {
                MutantTextField("SEARCH", query, { query = it }, placeholder = "Exercise, machine or pattern", testTag = "program_exercise_search")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MuscleFilters) { m -> MutantChoiceChip(m, muscle == m, { muscle = m }, modifier = Modifier.testTag("program_muscle_$m")) }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    matches.forEach { ex ->
                        LibraryRow(
                            title = ex.baseName.ifBlank { ex.name },
                            detail = listOf(ex.muscleGroup, ex.movementPattern, "${ex.defaultWorkSets}×${ex.defaultRepMin}–${ex.defaultRepMax}")
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            tag = "program_exercise_option_${ex.id}"
                        ) {
                            chosen = ex
                            variantId = variants.firstOrNull { it.exerciseId == ex.id || it.exerciseStableId == ex.stableId }?.id ?: ""
                        }
                    }
                    if (matches.isEmpty()) Text("No match in the library.", style = MutantType.BodySmall, color = MutantColors.TextMetadata,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                }
            } else {
                val options = variants.filter { it.exerciseId == current.id || it.exerciseStableId == current.stableId }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MutantEyebrow(current.muscleGroup.uppercase(), color = MutantColors.Primary)
                        Text(current.baseName.ifBlank { current.name }, style = MutantType.Title, color = MutantColors.TextPrimary)
                    }
                    TextButton(onClick = { chosen = null }) { Text("Change", style = MutantType.ButtonSmall, color = MutantColors.Primary) }
                }
                MutantEyebrow("MACHINE", modifier = Modifier.padding(start = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { v ->
                        val selected = v.id == variantId
                        Surface(
                            onClick = { variantId = v.id },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) MutantColors.PrimarySelected else MutantColors.Background,
                            border = BorderStroke(1.dp, if (selected) MutantColors.Primary else MutantColors.OutlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("program_variant_${v.id}")
                        ) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(v.variantName, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                                    Text(listOf(v.manufacturer, v.resistanceType, "+${loadText(v.weightIncrementKg)} kg").filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MutantType.Caption, color = MutantColors.TextSecondary)
                                }
                                Icon(if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked, contentDescription = null,
                                    tint = if (selected) MutantColors.Primary else MutantColors.TextSecondary, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    if (options.isEmpty()) Text("No machine variants yet. Add your own below.", style = MutantType.BodySmall, color = MutantColors.TextSecondary)
                }
                MutantButton("Custom variant", onClick = { onCreateVariant(current) }, style = MutantButtonStyle.Surface,
                    icon = Icons.Rounded.Add, height = 48.dp, textStyle = MutantType.ButtonSmall,
                    modifier = Modifier.fillMaxWidth().testTag("program_custom_variant"))
                MutantButton("Add to $dayTitle", onClick = { onAdd(current, variantId) },
                    modifier = Modifier.fillMaxWidth().testTag("confirm_add_program_exercise"))
            }
        }
    }
}

@Composable
private fun LibraryRow(title: String, detail: String, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MutantColors.Background,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    ) {
        Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                Text(detail, style = MutantType.MonoLabel.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                    color = MutantColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.AddCircle, contentDescription = "Choose $title", tint = MutantColors.Primary, modifier = Modifier.size(24.dp))
        }
    }
}

private fun loadText(value: Float): String = java.math.BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

data class CustomVariantInput(
    val name: String,
    val manufacturer: String,
    val resistanceType: String,
    val seat: String,
    val handle: String
)

@Composable
fun CustomVariantSheet(exercise: Exercise, onSave: (CustomVariantInput) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("${exercise.baseName.ifBlank { exercise.name }} — custom") }
    var manufacturer by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Plate-Loaded") }
    var seat by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("custom_variant_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Custom variant", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("For ${exercise.name}. Use it when your gym's machine isn't in the catalog.", style = MutantType.BodySmall,
                color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-4).dp))
            MutantTextField("NAME", name, { name = it }, testTag = "custom_variant_name")
            MutantTextField("BRAND / MACHINE", manufacturer, { manufacturer = it }, placeholder = "e.g. Panatta, Hammer Strength")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MutantEyebrow("RESISTANCE", modifier = Modifier.padding(start = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Plate-Loaded", "Selectorized", "Cable", "Smith", "Free Weight")) { option ->
                        MutantChoiceChip(option, type == option, { type = option })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutantTextField("SEAT", seat, { seat = it }, placeholder = "e.g. 4", modifier = Modifier.weight(1f))
                MutantTextField("GRIP", handle, { handle = it }, placeholder = "e.g. neutral", modifier = Modifier.weight(1f))
            }
            MutantButton(
                "Save variant",
                onClick = { onSave(CustomVariantInput(name.trim(), manufacturer.trim().ifBlank { "Gym machine" }, type, seat.trim(), handle.trim())) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("save_custom_variant")
            )
        }
    }
}

@Composable
fun CreateProgramSheet(onCreate: (name: String, description: String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("create_program_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New program", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("Starts with a 5-day split and two rest days. Add exercises to each day from Protocol.",
                style = MutantType.BodySmall, color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-4).dp))
            MutantTextField("NAME", name, { name = it }, placeholder = "e.g. Push / Pull / Legs", testTag = "program_name_input")
            MutantTextField("NOTES", description, { description = it }, placeholder = "Optional", singleLine = false, minHeight = 80.dp)
            MutantButton("Create program", onClick = { onCreate(name.trim(), description.trim()) }, enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag("confirm_create_program"))
        }
    }
}
