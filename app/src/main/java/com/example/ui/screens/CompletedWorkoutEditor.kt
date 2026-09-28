package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WorkoutSet
import com.example.data.model.formatLoad
import com.example.ui.designsystem.components.MutantPrimaryButton
import com.example.ui.viewmodel.MutantViewModel

/** Edits a detached completed-session draft; the view model owns transactional saving. */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("Edit workout") },
                navigationIcon = {
                    IconButton(onClick = leave, enabled = !saving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface {
                Column(Modifier.navigationBarsPadding().imePadding().padding(16.dp)) {
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp)) }
                    MutantPrimaryButton(
                        text = "Save changes", onClick = viewModel::saveWorkoutEdit,
                        enabled = current.session.title.isNotBlank() && invalidSets.values.none { it },
                        isLoading = saving, testTag = "save_workout_edit"
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = current.session.title,
                    onValueChange = { viewModel.updateWorkoutDraft(current.copy(session = current.session.copy(title = it))) },
                    label = { Text("Workout title") }, singleLine = true, enabled = !saving,
                    isError = current.session.title.isBlank(), modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = current.session.notes,
                    onValueChange = { viewModel.updateWorkoutDraft(current.copy(session = current.session.copy(notes = it))) },
                    label = { Text("Workout notes") }, enabled = !saving,
                    modifier = Modifier.fillMaxWidth(), minLines = 2
                )
            }
            current.exercises.forEach { detail ->
                item(key = "exercise-${detail.workoutExercise.id}") {
                    Text(detail.exercise.name, style = MaterialTheme.typography.titleMedium)
                    if (detail.sets.isEmpty()) Text("No sets recorded", style = MaterialTheme.typography.bodyMedium)
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
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your recorded workout will keep its saved values.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; viewModel.cancelWorkoutEdit() }) { Text("Discard changes") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Set ${set.setNumber} - ${if (set.setType == com.example.data.model.SetType.WARMUP) "Warm-up" else "Work"}",
            style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, enabled = enabled,
                singleLine = true, isError = !weightValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, enabled = enabled,
                singleLine = true, isError = !repsValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            OutlinedTextField(rir, { rir = it }, label = { Text("RIR") }, enabled = enabled,
                singleLine = true, isError = !rirValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        }
        if (!weightValid || !repsValid || !rirValid) Text(
            "Enter a weight of 0 or more, at least 1 rep, and RIR from 0 to 10.",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
        )
    }
}
