package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SetType
import com.example.data.model.WorkoutSet
import com.example.data.model.formatLoad
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.MutantViewModel

/** Edits a detached completed-session draft; the view model owns transactional saving. */
@Composable
fun CompletedWorkoutEditor(viewModel: MutantViewModel) {
    val draft by viewModel.workoutDraft.collectAsState()
    val current = draft ?: return
    val original = remember(current.session.id) { current }
    val saving by viewModel.isSavingWorkout.collectAsState()
    val error by viewModel.editError.collectAsState()
    val invalidSets = remember(current.session.id) { mutableStateMapOf<Long, Boolean>() }
    var confirmDiscard by remember(current.session.id) { mutableStateOf(false) }
    val leave = {
        if (!saving) {
            if (current != original || invalidSets.values.any { it }) confirmDiscard = true
            else viewModel.cancelWorkoutEdit()
        }
    }
    BackHandler { leave() }
    Scaffold(
        containerColor = MutantColors.Background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = leave, enabled = !saving) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MutantColors.TextSecondary)
                }
                Text("Edit workout", style = MutantType.Title, color = MutantColors.TextPrimary)
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MutantColors.Background)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                error?.let { Text(it, style = MutantType.BodySmall, color = MutantColors.Error) }
                MutantButton(
                    if (saving) "Saving…" else "Save changes", onClick = viewModel::saveWorkoutEdit,
                    enabled = current.session.title.isNotBlank() && invalidSets.values.none { it },
                    loading = saving, modifier = Modifier.fillMaxWidth().testTag("save_workout_edit")
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MutantTextField(
                    "WORKOUT", current.session.title,
                    { viewModel.updateWorkoutDraft(current.copy(session = current.session.copy(title = it))) },
                    enabled = !saving, testTag = "edit_workout_title"
                )
                if (current.session.title.isBlank()) Text("Give the workout a name.", style = MutantType.Caption, color = MutantColors.Error,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp))
            }
            item {
                MutantTextField(
                    "NOTES", current.session.notes,
                    { viewModel.updateWorkoutDraft(current.copy(session = current.session.copy(notes = it))) },
                    placeholder = "Optional", singleLine = false, minHeight = 80.dp, enabled = !saving
                )
            }
            current.exercises.forEach { detail ->
                item(key = "exercise-${detail.workoutExercise.id}") {
                    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(detail.exercise.name, style = MutantType.Title, color = MutantColors.TextPrimary)
                        if (detail.sets.isEmpty()) Text("No sets recorded", style = MutantType.BodySmall, color = MutantColors.TextSecondary)
                    }
                }
                items(detail.sets, key = { "set-${it.id}" }) { set ->
                    CompletedSetEditor(set, enabled = !saving, onValidityChanged = { invalidSets[set.id] = !it }) { updated ->
                        viewModel.updateWorkoutDraft(current.copy(exercises = current.exercises.map { exercise ->
                            if (exercise.workoutExercise.id != detail.workoutExercise.id) exercise
                            else exercise.copy(sets = exercise.sets.map { if (it.id == set.id) updated else it })
                        }))
                    }
                }
            }
        }
    }
    if (confirmDiscard) {
        MutantBottomSheet(onDismiss = { confirmDiscard = false }, modifier = Modifier.testTag("discard_edit_sheet")) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Discard changes?", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
                Text("The recorded workout keeps its saved values.", style = MutantType.Body, color = MutantColors.TextSecondary)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MutantButton("Keep editing", onClick = { confirmDiscard = false }, style = MutantButtonStyle.Outline, height = 54.dp,
                        textStyle = MutantType.Button.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                    MutantButton("Discard", onClick = { confirmDiscard = false; viewModel.cancelWorkoutEdit() },
                        style = MutantButtonStyle.Danger, height = 54.dp, textStyle = MutantType.Button.copy(fontSize = 15.sp),
                        modifier = Modifier.weight(1f).testTag("confirm_discard_edit"))
                }
            }
        }
    }
}

@Composable
private fun CompletedSetEditor(
    set: WorkoutSet,
    enabled: Boolean,
    onValidityChanged: (Boolean) -> Unit,
    onChange: (WorkoutSet) -> Unit
) {
    var weight by rememberSaveable(set.id) { mutableStateOf(set.weightKg.formatLoad()) }
    var reps by rememberSaveable(set.id) { mutableStateOf(set.reps.toString()) }
    var rir by rememberSaveable(set.id) { mutableStateOf(set.rir.toString()) }
    val parsedWeight = weight.replace(',', '.').toFloatOrNull()
    val parsedReps = reps.toIntOrNull()
    val parsedRir = rir.toIntOrNull()
    val weightValid = parsedWeight != null && parsedWeight.isFinite() && parsedWeight >= 0
    val repsValid = parsedReps != null && parsedReps > 0
    val rirValid = parsedRir != null && parsedRir in 0..10
    LaunchedEffect(weight, reps, rir) {
        onValidityChanged(weightValid && repsValid && rirValid)
        if (weightValid && repsValid && rirValid) {
            onChange(set.copy(weightKg = parsedWeight!!, reps = parsedReps!!, rir = parsedRir!!))
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(18.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MutantEyebrow("SET ${set.setNumber} · ${if (set.setType == SetType.WARMUP) "WARM-UP" else "WORK"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutantTextField("KG", weight, { weight = it }, enabled = enabled, keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            MutantTextField("REPS", reps, { reps = it }, enabled = enabled, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
            MutantTextField("RIR", rir, { rir = it }, enabled = enabled, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        if (!weightValid || !repsValid || !rirValid) Text(
            "Enter a weight of 0 or more, at least 1 rep, and RIR from 0 to 10.",
            style = MutantType.Caption, color = MutantColors.Error
        )
    }
}
