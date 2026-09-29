package com.example.ui.screens

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.ReadinessInput
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    com.example.ui.designsystem.WorkoutLoggerTheme {
        ActiveWorkoutScreenState(viewModel, onNavigateBack, onNavigateToHistory, modifier)
    }
}

@Composable
private fun ActiveWorkoutScreenState(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier
) {
    val state by viewModel.activeWorkoutUiState.collectAsState()
    val isDiscarding by viewModel.isDiscardingWorkout.collectAsState()
    val isFinishing by viewModel.isFinishingWorkout.collectAsState()
    val discardError by viewModel.discardError.collectAsState()
    var showDiscardConfirmation by remember(state.session?.id) { mutableStateOf(false) }
    if (showDiscardConfirmation && state.session != null) {
        DiscardWorkoutDialog(
            isDiscarding = isDiscarding,
            error = discardError,
            onDismiss = { showDiscardConfirmation = false },
            onDiscard = { viewModel.discardWorkout(onNavigateBack) }
        )
    }

    val systemStatus by viewModel.systemStatus.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val isStartingWorkout by viewModel.isStartingWorkout.collectAsState()
    val startWorkoutError by viewModel.startWorkoutError.collectAsState()
    var showFreeSessionDialog by remember { mutableStateOf(false) }

    if (showFreeSessionDialog) {
        StartFreeSessionDialog(
            exercises = allExercises,
            isStarting = isStartingWorkout,
            error = startWorkoutError,
            onDismiss = { showFreeSessionDialog = false },
            onStart = { title, exerciseIds ->
                viewModel.startAdHocWorkout(
                    title = title,
                    exerciseIds = exerciseIds,
                    readiness = ReadinessInput()
                ) {
                    showFreeSessionDialog = false
                }
            }
        )
    }

    val session = state.session
    val coroutineScope = rememberCoroutineScope()

    if (state.isLoading) {
        com.example.ui.designsystem.components.MutantLoadingScreen(
            modifier = modifier.testTag("active_workout_loading"),
            message = "Loading workout...",
            subMessage = "Carregando exercícios e séries..."
        )
        return
    }

    if (session == null || state.exercises.isEmpty()) {
        NoActiveWorkoutContent(
            lastSessionTitle = systemStatus.lastSessionTitle,
            lastSessionDaysAgo = systemStatus.lastSessionDaysAgo,
            hasEmptySession = session != null && state.exercises.isEmpty(),
            isDiscarding = isDiscarding,
            onNavigateToProtocols = onNavigateBack,
            onNavigateToHistory = onNavigateToHistory,
            onStartFreeSession = { showFreeSessionDialog = true },
            onDiscardWorkout = {
                viewModel.clearDiscardError()
                showDiscardConfirmation = true
            },
            modifier = modifier
        )
        return
    }

    val currentExDetail = state.exercises.getOrNull(state.currentExerciseIndex) ?: state.exercises.first()
    val exercise = currentExDetail.exercise
    val sets = currentExDetail.sets
    val progression = state.currentProgression

    // Fast logging state
    var selectedSetType by remember(currentExDetail.workoutExercise.id) { mutableStateOf(SetType.WORK) }
    var weightInput by remember(currentExDetail.workoutExercise.id, progression?.suggestedWeightKg) {
        val initialWeight = currentExDetail.workoutExercise.nextSetWeightKg ?: progression?.suggestedWeightKg ?: 60f
        mutableStateOf(if (initialWeight % 1f == 0f) initialWeight.toInt().toString() else initialWeight.toString())
    }
    var repsInput by remember(currentExDetail.workoutExercise.id, progression?.suggestedRepsMin) {
        val initialReps = currentExDetail.workoutExercise.nextSetReps ?: progression?.suggestedRepsMin ?: exercise.defaultRepMin
        mutableStateOf(initialReps.toString())
    }
    LaunchedEffect(currentExDetail.workoutExercise.id, weightInput, repsInput) {
        val weight = weightInput.toFloatOrNull()
        val reps = repsInput.toIntOrNull()
        if (weight != null && weight.isFinite() && weight >= 0f && reps != null && reps > 0) viewModel.updateNextSet(currentExDetail.workoutExercise.id, weight, reps)
    }
    var selectedRir by remember(currentExDetail.workoutExercise.id) { mutableStateOf(exercise.defaultRir) }
    var selectedTechnique by remember(currentExDetail.workoutExercise.id) { mutableStateOf(IntensityTechnique.NONE) }
    var attachedSegments by remember(currentExDetail.workoutExercise.id) { mutableStateOf<List<SetSegment>>(emptyList()) }

    // Motion states
    var isLogButtonPressed by remember { mutableStateOf(false) }
    // Dialogs & Collapsibles
    var showIntensityDialog by remember(currentExDetail.workoutExercise.id) { mutableStateOf(false) }
    var showCues by remember(currentExDetail.workoutExercise.id) { mutableStateOf(false) }
    var showSetupEditor by remember(currentExDetail.workoutExercise.id) { mutableStateOf(false) }
    var showFinishConfirmation by remember(session.id) { mutableStateOf(false) }
    val finishRequested by viewModel.finishRequested.collectAsState()
    LaunchedEffect(finishRequested, session.id) {
        if (finishRequested) {
            showFinishConfirmation = true
            viewModel.consumeFinishRequest()
        }
    }
    var showTargetExplanationDialog by remember { mutableStateOf(false) }
    var showPostSetFeedback by remember(currentExDetail.workoutExercise.id) { mutableStateOf(false) }
    var lastSavedSetSummary by remember(currentExDetail.workoutExercise.id) { mutableStateOf("") }
    var feedbackCollapsed by remember(currentExDetail.workoutExercise.id) { mutableStateOf(false) }

    // Execution & Muscle Quality State for Current Exercise
    var executionQuality by remember(currentExDetail.workoutExercise.id, currentExDetail.workoutExercise.executionQuality) {
        mutableStateOf(currentExDetail.workoutExercise.executionQuality)
    }
    var targetMuscleQuality by remember(currentExDetail.workoutExercise.id, currentExDetail.workoutExercise.targetMuscleQuality) {
        mutableStateOf(currentExDetail.workoutExercise.targetMuscleQuality)
    }
    var seatPos by remember(currentExDetail.workoutExercise.id, currentExDetail.workoutExercise.seatPosition) {
        mutableStateOf(currentExDetail.workoutExercise.seatPosition)
    }
    var handlePos by remember(currentExDetail.workoutExercise.id, currentExDetail.workoutExercise.handlePosition) {
        mutableStateOf(currentExDetail.workoutExercise.handlePosition)
    }
    var exerciseNotes by remember(currentExDetail.workoutExercise.id, currentExDetail.workoutExercise.notes) {
        mutableStateOf(currentExDetail.workoutExercise.notes)
    }

    val warmUpSets = sets.filter { it.setType == SetType.WARMUP }
    val workSets = sets.filter { it.setType == SetType.WORK }

    if (showIntensityDialog) {
        IntensitySegmentDialog(
            initialWeight = weightInput.toFloatOrNull() ?: 60f,
            initialReps = repsInput.toIntOrNull() ?: 10,
            techniqueName = if (selectedTechnique == IntensityTechnique.REST_PAUSE) "Rest-pause" else "Drop set",
            onDismiss = { showIntensityDialog = false },
            onSaveSegments = { segments ->
                attachedSegments = segments
                showIntensityDialog = false
            }
        )
    }

    if (showTargetExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showTargetExplanationDialog = false },
            title = {
                Text(
                    text = "Double Progression Protocol",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = MutantVolt
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)) {
                    Text(
                        text = "Target Protocol: ${exercise.defaultWorkSets} work sets · ${exercise.defaultRepMin}–${exercise.defaultRepMax} reps @ ${exercise.defaultRir} RIR",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary)
                    )
                    Text(
                        text = "Regra do Sistema:\n${progression?.reason ?: "Aumentar peso apenas quando todas as séries de trabalho atingirem o topo da faixa com boa execução."}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                    )
                    Text(
                        text = "Incremento: +${exercise.defaultIncrementKg} kg. Séries de aquecimento não influenciam a progressão. Execução comprometida congela a carga.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextMuted)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTargetExplanationDialog = false }) {
                    Text("OK", color = MutantVolt, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }

    val activeExDetail = currentExDetail
    val activeEx = exercise
    ActiveWorkoutContent(
        state = state,
        weightValue = weightInput,
        repsValue = repsInput,
        setType = selectedSetType,
        rir = selectedRir,
        technique = selectedTechnique,
        seatPosition = seatPos,
        handlePosition = handlePos,
        segments = attachedSegments,
        enabled = !isDiscarding && !isFinishing && !isLogButtonPressed,
        onBack = onNavigateBack,
        onFinish = { showFinishConfirmation = true },
        onDiscard = { viewModel.clearDiscardError(); showDiscardConfirmation = true },
        onSelectExercise = viewModel::setCurrentExerciseIndex,
        onSetup = { showSetupEditor = !showSetupEditor },
        onCues = { showCues = !showCues },
        onTarget = { showTargetExplanationDialog = true },
        onWeightChange = { weightInput = it },
        onRepsChange = { repsInput = it },
        onSetTypeChange = { selectedSetType = it },
        onRirChange = { selectedRir = it },
        onTechniqueChange = {
            if (selectedTechnique != it) attachedSegments = emptyList()
            selectedTechnique = it
            if (it == IntensityTechnique.REST_PAUSE || it == IntensityTechnique.DROP_SET) showIntensityDialog = true
        },
        onEditSegments = { showIntensityDialog = true },
        onLogSet = {
            val weight = weightInput.toFloatOrNull()
            val reps = repsInput.toIntOrNull()
            if (weight != null && weight.isFinite() && weight >= 0f && reps != null && reps > 0 && !isLogButtonPressed) {
                viewModel.logSet(
                    workoutExerciseId = currentExDetail.workoutExercise.id,
                    type = selectedSetType, weightKg = weight, reps = reps, rir = selectedRir,
                    technique = selectedTechnique, segments = attachedSegments,
                    defaultRestSeconds = exercise.defaultRestSeconds, muscleGroup = exercise.muscleGroup
                )
                if (selectedSetType == SetType.WORK) {
                    lastSavedSetSummary = "${weight.formatLoad()} kg ? $reps @ $selectedRir saved"
                    feedbackCollapsed = false
                    showPostSetFeedback = true
                }
                attachedSegments = emptyList()
                selectedTechnique = IntensityTechnique.NONE
                isLogButtonPressed = true
                coroutineScope.launch {
                    delay(MotionDuration.Feedback.toLong())
                    isLogButtonPressed = false
                }
            }
        },
        modifier = modifier,
        showFeedback = showPostSetFeedback,
        setupEditor = {
            AnimatedVisibility(
                visible = showSetupEditor,
                enter = MutantMotion.CollapsibleEnterTransition,
                exit = MutantMotion.CollapsibleExitTransition
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MutantSpacing.xs),
                    shape = MutantShapeTokens.CompactControl,
                    color = MutantSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                ) {
                    Column(modifier = Modifier.padding(MutantSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                        ) {
                            OutlinedTextField(
                                value = seatPos,
                                onValueChange = { seatPos = it },
                                label = { Text("Seat Position", fontSize = MutantTypeScale.label) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = handlePos,
                                onValueChange = { handlePos = it },
                                label = { Text("Handle Position", fontSize = MutantTypeScale.label) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(MutantSpacing.xs))
                        OutlinedTextField(
                            value = exerciseNotes,
                            onValueChange = { exerciseNotes = it },
                            label = { Text("Gym notes") },
                            modifier = Modifier.fillMaxWidth().testTag("exercise_notes_input"),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(MutantSpacing.xs))
                        Button(
                            onClick = {
                                viewModel.updateExerciseDetails(
                                    activeExDetail.workoutExercise.id,
                                    executionQuality,
                                    targetMuscleQuality,
                                    seatPos,
                                    handlePos,
                                    exerciseNotes
                                )
                                showSetupEditor = false
                            },
                            modifier = Modifier.align(Alignment.End),
                            colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                        ) {
                            Text("Save Setup", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showCues,
                enter = MutantMotion.CollapsibleEnterTransition,
                exit = MutantMotion.CollapsibleExitTransition
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MutantSpacing.xs),
                    shape = MutantShapeTokens.CompactControl,
                    color = MutantDarkNavy,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                ) {
                    Text(
                        text = activeEx.executionCues,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MutantTextSecondary,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.padding(MutantSpacing.sm)
                    )
                }
            }
        },
        feedback = {
            if (showPostSetFeedback) {
                AnimatedVisibility(
                    visible = showPostSetFeedback,
                    enter = MutantMotion.CollapsibleEnterTransition,
                    exit = MutantMotion.CollapsibleExitTransition
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MutantShapeTokens.InputChip,
                        color = MutantDarkNavy,
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(MutantSpacing.mdPlus)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = lastSavedSetSummary,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantVolt
                                    )
                                )
                                TextButton(
                                    onClick = { showPostSetFeedback = false },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("✓ Dismiss", color = MutantEmerald, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (!feedbackCollapsed) {
                                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                                Text(
                                    text = "Execution",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                )
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                                ) {
                                    listOf("Excellent", "Good", "Compromised", "Bad").forEach { q ->
                                        val isSelected = executionQuality.equals(q, ignoreCase = true)
                                        val qColor = when (q) {
                                            "Excellent" -> MutantEmerald
                                            "Good" -> MutantCyan
                                            "Compromised" -> MutantAmber
                                            else -> MutantRed
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clickable {
                                                    executionQuality = q
                                                    viewModel.updateExerciseDetails(
                                                        activeExDetail.workoutExercise.id,
                                                        q,
                                                        targetMuscleQuality,
                                                        seatPos,
                                                        handlePos,
                                                        activeExDetail.workoutExercise.notes
                                                    )
                                                    coroutineScope.launch {
                                                        delay(180)
                                                        feedbackCollapsed = true
                                                        delay(1200)
                                                        showPostSetFeedback = false
                                                        feedbackCollapsed = false
                                                    }
                                                },
                                            shape = MutantShapeTokens.SmallControl,
                                            color = if (isSelected) qColor.copy(alpha = 0.25f) else MutantSurfaceCard,
                                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSelected) qColor else MutantBorder)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = q,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) qColor else MutantTextSecondary,
                                                        fontSize = MutantTypeScale.micro
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(MutantSpacing.compactMd))
                                Text(
                                    text = "Target muscle sensation",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                )
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                                ) {
                                    listOf("Excellent", "Good", "Weak", "None").forEach { m ->
                                        val isSelected = targetMuscleQuality.equals(m, ignoreCase = true)
                                        val mColor = when (m) {
                                            "Excellent" -> MutantEmerald
                                            "Good" -> MutantCyan
                                            "Weak" -> MutantAmber
                                            else -> MutantRed
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(30.dp)
                                                .clickable {
                                                    targetMuscleQuality = m
                                                    viewModel.updateExerciseDetails(
                                                        activeExDetail.workoutExercise.id,
                                                        executionQuality,
                                                        m,
                                                        seatPos,
                                                        handlePos,
                                                        activeExDetail.workoutExercise.notes
                                                    )
                                                },
                                            shape = MutantShapeTokens.SmallControl,
                                            color = if (isSelected) mColor.copy(alpha = 0.25f) else MutantSurfaceCard,
                                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSelected) mColor else MutantBorder)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = m,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) mColor else MutantTextSecondary,
                                                        fontSize = MutantTypeScale.micro
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Text(
                                    text = "✓ $executionQuality execution · $targetMuscleQuality target sensation",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutantEmerald, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        },
        recordedSets = {
            Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                if (warmUpSets.isNotEmpty()) {
                    Text("WARM-UP SETS", style = com.example.ui.designsystem.WorkoutLoggerTokens.Label, color = MutantTextMuted)
                    warmUpSets.forEachIndexed { index, set ->
                        SetRowCard(set, index + 1, executionQuality) { viewModel.deleteSet(set.id) }
                    }
                }
                if (workSets.isNotEmpty()) {
                    Text("WORK SETS ? ${workSets.size} / ${exercise.defaultWorkSets}",
                        style = com.example.ui.designsystem.WorkoutLoggerTokens.Label, color = MutantEmerald)
                    workSets.forEachIndexed { index, set ->
                        SetRowCard(set, index + 1, executionQuality) { viewModel.deleteSet(set.id) }
                    }
                }
            }
        },
        restTimer = {
            RestTimerBar(
                remainingSeconds = state.restTimerRemainingSeconds, isRunning = state.isRestTimerRunning,
                isComplete = state.restTimerCompleted, recommendedText = state.restTimerRecommended,
                exerciseName = exercise.name, nextSetText = "Work set ${workSets.size + 1} / ${exercise.defaultWorkSets}",
                onAdjustTime = viewModel::adjustRestTimer, onTogglePlayPause = viewModel::toggleRestTimer,
                onSkip = viewModel::skipRestTimer, modifier = Modifier.padding(MutantSpacing.sm)
            )
        }
    )

    // Finish Workout Confirmation Dialog (Focal completion sequence)
    if (showFinishConfirmation) {
        var workoutNotes by remember { mutableStateOf("") }
        var bodyweightInput by remember { mutableStateOf("108.5") }

        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = {
                Text(
                    text = "CONCLUDE SESSION",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MutantVolt)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)) {
                    Text(
                        text = "Protocol execution complete. The system will audit progressive overload and update next session targets.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MutantTextSecondary)
                    )
                    OutlinedTextField(
                        value = bodyweightInput,
                        onValueChange = { bodyweightInput = it },
                        label = { Text("Bodyweight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutantVolt,
                            unfocusedBorderColor = MutantBorder
                        )
                    )
                    OutlinedTextField(
                        value = workoutNotes,
                        onValueChange = { workoutNotes = it },
                        label = { Text("Session Notes / Energy / Pump") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutantVolt,
                            unfocusedBorderColor = MutantBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bw = bodyweightInput.toFloatOrNull() ?: 0f
                        viewModel.finishWorkout(workoutNotes, bw) {
                            showFinishConfirmation = false
                            onNavigateBack()
                        }
                    },
                    enabled = !isFinishing,
                    colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                ) {
                    if (isFinishing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MutantOnVolt
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FINALIZANDO...", fontWeight = FontWeight.Black)
                    } else {
                        Text("SAVE & AUDIT", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isFinishing) showFinishConfirmation = false }, enabled = !isFinishing) {
                    Text("Cancel", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }
}

@Composable
private fun SetRowCard(
    set: WorkoutSet,
    displayIndex: Int,
    executionQuality: String,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("set_row_${set.id}"),
        shape = MutantShapeTokens.TinyControl,
        color = MutantDarkNavy,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (set.setType == SetType.WORK) MutantEmerald.copy(alpha = 0.4f) else MutantBorder
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = MutantSpacing.mdPlus, vertical = MutantSpacing.compactMd)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = if (set.setType == SetType.WORK) MutantEmerald else MutantTextMuted
                        )
                    )
                    Spacer(modifier = Modifier.width(MutantSpacing.xs))
                    Text(
                        text = if (set.setType == SetType.WORK) {
                            "${if (set.weightKg % 1f == 0f) set.weightKg.toInt() else set.weightKg} × ${set.reps} @ ${set.rir}"
                        } else {
                            "${if (set.weightKg % 1f == 0f) set.weightKg.toInt() else set.weightKg} × ${set.reps}"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MutantTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    if (set.setType == SetType.WORK) {
                        Spacer(modifier = Modifier.height(MutantSpacing.compactMd))
                        Text(
                            text = executionQuality,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (executionQuality in listOf("Excellent", "Good")) MutantEmerald else MutantAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (set.isPr) {
                        Surface(
                            color = MutantAmber.copy(alpha = 0.2f),
                            shape = MutantShapeTokens.Compact,
                            modifier = Modifier.padding(end = MutantSpacing.compact)
                        ) {
                            Text(
                                text = "PR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MutantAmber,
                                    fontSize = MutantTypeScale.micro
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.setBadgeHorizontal, vertical = MutantSpacing.setBadgeVertical)
                            )
                        }
                    }

                    if (set.technique != IntensityTechnique.NONE) {
                        Surface(
                            color = MutantAmber.copy(alpha = 0.15f),
                            shape = MutantShapeTokens.Compact,
                            modifier = Modifier.padding(end = MutantSpacing.compact)
                        ) {
                            Text(
                                text = set.technique.name.replace("_", "-").lowercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantAmber,
                                    fontSize = MutantTypeScale.micro
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.setBadgeHorizontal, vertical = MutantSpacing.setBadgeVertical)
                            )
                        }
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete set",
                            tint = MutantTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = MutantSpacing.xs)) {
                    HorizontalDivider(color = MutantBorder.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(MutantSpacing.compact))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (set.setType == SetType.WORK) "Work Set #$displayIndex" else "Warm-up Set #$displayIndex",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                        )
                        Text(
                            text = "Intensity: ${set.technique.name.replace("_", " ")}",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                        )
                    }
                }
            }
        }
    }
}
