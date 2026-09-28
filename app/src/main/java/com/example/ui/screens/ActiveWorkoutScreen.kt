package com.example.ui.screens

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ActiveWorkoutUiState
import com.example.ui.viewmodel.MutantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActiveWorkoutScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.activeWorkoutUiState.collectAsState()
    val isDiscarding by viewModel.isDiscardingWorkout.collectAsState()
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

    val session = state.session
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    if (state.isLoading) {
        Box(modifier.fillMaxSize().background(MutantBlack), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator(color = MutantVolt)
                Text("Loading workout...", color = MutantTextSecondary)
            }
        }
        return
    }

    if (session == null || state.exercises.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MutantBlack)
                .padding(MutantSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (session == null) "NO ACTIVE WORKOUT" else "NO EXERCISES",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                Text(
                    text = if (session == null) "Select a protocol day on the Home screen to initiate a session."
                        else "This workout has no exercises. Discard it and add exercises to your protocol.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MutantTextMuted)
                )
                if (session != null) {
                    TextButton(enabled = !isDiscarding, onClick = { viewModel.clearDiscardError(); showDiscardConfirmation = true }) {
                        Text("Discard workout", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(MutantSpacing.lgMd))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                ) {
                    Text("GO TO HOME", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    val currentExDetail = state.exercises.getOrNull(state.currentExerciseIndex) ?: state.exercises.first()
    val exercise = currentExDetail.exercise
    val sets = currentExDetail.sets
    val progression = state.currentProgression

    // Fast logging state
    var selectedSetType by remember { mutableStateOf(SetType.WORK) }
    var weightInput by remember(exercise.id, progression?.suggestedWeightKg) {
        val initialWeight = currentExDetail.workoutExercise.nextSetWeightKg ?: progression?.suggestedWeightKg ?: 60f
        mutableStateOf(if (initialWeight % 1f == 0f) initialWeight.toInt().toString() else initialWeight.toString())
    }
    var repsInput by remember(exercise.id, progression?.suggestedRepsMin) {
        val initialReps = currentExDetail.workoutExercise.nextSetReps ?: progression?.suggestedRepsMin ?: exercise.defaultRepMin
        mutableStateOf(initialReps.toString())
    }
    LaunchedEffect(currentExDetail.workoutExercise.id, weightInput, repsInput) {
        val weight = weightInput.toFloatOrNull()
        val reps = repsInput.toIntOrNull()
        if (weight != null && reps != null) viewModel.updateNextSet(currentExDetail.workoutExercise.id, weight, reps)
    }
    var selectedRir by remember(exercise.id) { mutableStateOf(exercise.defaultRir) }
    var selectedTechnique by remember(exercise.id) { mutableStateOf(IntensityTechnique.NONE) }
    var attachedSegments by remember(exercise.id) { mutableStateOf<List<SetSegment>>(emptyList()) }

    // Motion states
    var isLogButtonPressed by remember { mutableStateOf(false) }
    val logButtonScale by animateFloatAsState(
        targetValue = if (isLogButtonPressed) 0.98f else 1f,
        animationSpec = tween(MotionDuration.Feedback),
        label = "LogButtonScale"
    )

    // Dialogs & Collapsibles
    var showIntensityDialog by remember { mutableStateOf(false) }
    var showCues by remember { mutableStateOf(false) }
    var showSetupEditor by remember { mutableStateOf(false) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    val finishRequested by viewModel.finishRequested.collectAsState()
    LaunchedEffect(finishRequested, session.id) {
        if (finishRequested) {
            showFinishConfirmation = true
            viewModel.consumeFinishRequest()
        }
    }
    var showTargetExplanationDialog by remember { mutableStateOf(false) }
    var showPostSetFeedback by remember { mutableStateOf(false) }
    var lastSavedSetSummary by remember { mutableStateOf("") }
    var feedbackCollapsed by remember { mutableStateOf(false) }

    // Execution & Muscle Quality State for Current Exercise
    var executionQuality by remember(currentExDetail.workoutExercise.executionQuality) {
        mutableStateOf(currentExDetail.workoutExercise.executionQuality)
    }
    var targetMuscleQuality by remember(currentExDetail.workoutExercise.targetMuscleQuality) {
        mutableStateOf(currentExDetail.workoutExercise.targetMuscleQuality)
    }
    var seatPos by remember(currentExDetail.workoutExercise.seatPosition) {
        mutableStateOf(currentExDetail.workoutExercise.seatPosition)
    }
    var handlePos by remember(currentExDetail.workoutExercise.handlePosition) {
        mutableStateOf(currentExDetail.workoutExercise.handlePosition)
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MutantBlack,
        topBar = {
            Surface(
                color = MutantDarkNavy,
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = MutantSpacing.md, vertical = MutantSpacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MutantTextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(MutantSpacing.compact))
                            Column {
                                Text(
                                    text = session.title.uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MutantVolt,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )
                                Text(
                                    text = "Exercise ${state.currentExerciseIndex + 1} of ${state.exercises.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                )
                            }
                        }

                        Button(
                            enabled = !isDiscarding,
                            onClick = { showFinishConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutantSurfaceElevated,
                                contentColor = MutantVolt
                            ),
                            shape = MutantShapeTokens.TinyControl,
                            contentPadding = PaddingValues(horizontal = MutantSpacing.sm, vertical = MutantSpacing.compact)
                        ) {
                            Text("FINISH", fontWeight = FontWeight.Bold, fontSize = MutantTypeScale.compact)
                        }
                    }


                    // Expressive exercise progress with directly selectable steps.
                    Spacer(modifier = Modifier.height(MutantSpacing.compactMd))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.Canvas(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MutantSpacing.lg).height(24.dp)
                        ) {
                            val centerY = size.height / 2f
                            drawLine(
                                color = MutantBorder,
                                start = androidx.compose.ui.geometry.Offset(0f, centerY),
                                end = androidx.compose.ui.geometry.Offset(size.width, centerY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            state.exercises.forEachIndexed { index, exDetail ->
                                val isCurrent = index == state.currentExerciseIndex
                                val isCompleted = exDetail.sets.count { it.setType == SetType.WORK } >= exDetail.exercise.defaultWorkSets
                                val nodeColor = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isCompleted -> MutantEmerald
                                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                                Column(
                                    modifier = Modifier.weight(1f).clickable { viewModel.setCurrentExerciseIndex(index) },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        modifier = Modifier.size(24.dp),
                                        shape = CircleShape,
                                        color = nodeColor,
                                        border = if (!isCurrent && !isCompleted) androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder) else null
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isCompleted && !isCurrent) {
                                                Icon(Icons.Default.Check, contentDescription = "Complete", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                                            } else if (isCurrent) {
                                                Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(MutantSpacing.micro))
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MutantTextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Floating Rest Timer Bar right at bottom of workout
            RestTimerBar(
                remainingSeconds = state.restTimerRemainingSeconds,
                isRunning = state.isRestTimerRunning,
                isComplete = state.restTimerCompleted,
                recommendedText = state.restTimerRecommended,
                exerciseName = exercise.name,
                nextSetText = "Work set ${workSets.size + 1} / ${exercise.defaultWorkSets}",
                onAdjustTime = { viewModel.adjustRestTimer(it) },
                onTogglePlayPause = { viewModel.toggleRestTimer() },
                onSkip = { viewModel.skipRestTimer() },
                modifier = Modifier.padding(MutantSpacing.sm)
            )
        }
    ) { innerPadding ->
        // AnimatedContent for exercise transitions (32dp shared-axis slide continuity)
        AnimatedContent(
            targetState = state.currentExerciseIndex,
            transitionSpec = {
                MutantMotion.exerciseTransitionSpec(density, targetState > initialState)
            },
            label = "ExerciseSharedAxisTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { exerciseIndex ->
            val activeExDetail = state.exercises.getOrNull(exerciseIndex) ?: currentExDetail
            val activeEx = activeExDetail.exercise
            val activeSets = activeExDetail.sets
            val activeWarmups = activeSets.filter { it.setType == SetType.WARMUP }
            val activeWorks = activeSets.filter { it.setType == SetType.WORK }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = MutantSpacing.md),
                contentPadding = PaddingValues(top = MutantSpacing.sm, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(MutantSpacing.mdPlus)
            ) {
                // 1. Exercise Name & Machine Setup
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = MutantSpacing.micro)
                    ) {
                        Text(
                            text = activeEx.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantTextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showSetupEditor = !showSetupEditor }
                                    .padding(vertical = MutantSpacing.micro)
                            ) {
                                Text(
                                    text = "Seat ${seatPos.ifEmpty { "4" }} · Handle ${handlePos.ifEmpty { "Wide" }}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantVolt
                                    )
                                )
                                Spacer(modifier = Modifier.width(MutantSpacing.compact))
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Edit setup",
                                    tint = MutantTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (activeEx.executionCues.isNotEmpty()) {
                                Text(
                                    text = if (showCues) "Hide cues" else "Cues",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantTextSecondary
                                    ),
                                    modifier = Modifier
                                        .clickable { showCues = !showCues }
                                        .padding(MutantSpacing.xxs)
                                    )
                            }
                        }

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
                                    Button(
                                        onClick = {
                                            viewModel.updateExerciseDetails(
                                                activeExDetail.workoutExercise.id,
                                                executionQuality,
                                                targetMuscleQuality,
                                                seatPos,
                                                handlePos,
                                                activeExDetail.workoutExercise.notes
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
                    }
                }

                // 2. LAST TIME Performance
                if (progression?.historyWorkSets?.isNotEmpty() == true) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MutantSpacing.micro)
                        ) {
                            Text(
                                text = "LAST TIME",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MutantTextMuted,
                                    letterSpacing = MutantTracking.Label
                                )
                            )
                            Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                            progression.historyWorkSets.forEach { hw ->
                                val weightStr = if (hw.weightKg % 1f == 0f) hw.weightKg.toInt().toString() else hw.weightKg.toString()
                                Text(
                                    text = "$weightStr × ${hw.reps} @ ${hw.rir}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MutantTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. TARGET Card (Compact & Tappable)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTargetExplanationDialog = true }
                            .testTag("target_banner"),
                        shape = MutantShapeTokens.InputChip,
                        color = MutantSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = MutantSpacing.mdPlus, vertical = MutantSpacing.compactMd),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TARGET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MutantVolt,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )
                                Spacer(modifier = Modifier.height(MutantSpacing.micro))
                                Text(
                                    text = "${activeEx.defaultWorkSets} × ${activeEx.defaultRepMin}–${activeEx.defaultRepMax} @ ${activeEx.defaultRir} RIR",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MutantTextPrimary
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                val suggestedWeight = progression?.suggestedWeightKg ?: 60f
                                val statusLabel = when (progression?.status) {
                                    ProgressionStatus.INCREASE_LOAD -> "Increase load (+${activeEx.defaultIncrementKg}kg)"
                                    ProgressionStatus.DELOAD_OR_CONSOLIDATE -> "Consolidate form"
                                    ProgressionStatus.FIRST_TIME -> "Baseline"
                                    else -> "Maintain load"
                                }
                                val statusColor = when (progression?.status) {
                                    ProgressionStatus.INCREASE_LOAD -> MutantEmerald
                                    ProgressionStatus.DELOAD_OR_CONSOLIDATE -> MutantAmber
                                    else -> MutantCyan
                                }

                                Text(
                                    text = "Today: ${if (suggestedWeight % 1f == 0f) suggestedWeight.toInt() else suggestedWeight} kg",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MutantVolt
                                    )
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(MutantSpacing.xxs))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Details",
                                        tint = MutantTextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Sets Logged (Warm-up & Work Sets)
                if (activeWarmups.isNotEmpty()) {
                    item {
                        Text(
                            text = "WARM-UP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantTextMuted,
                                letterSpacing = MutantTracking.Label
                            )
                        )
                    }

                    itemsIndexed(activeWarmups) { index, s ->
                        AnimatedVisibility(
                            visible = true,
                            enter = MutantMotion.SetEnterTransition,
                            exit = MutantMotion.SetExitTransition
                        ) {
                            SetRowCard(
                                set = s,
                                displayIndex = index + 1,
                                executionQuality = executionQuality,
                                onDelete = { viewModel.deleteSet(s.id) }
                            )
                        }
                    }
                }

                if (activeWorks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WORK SETS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MutantEmerald,
                                    letterSpacing = MutantTracking.Label
                                )
                            )
                            Text(
                                text = "${activeWorks.size} / ${activeEx.defaultWorkSets}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MutantTextMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    itemsIndexed(activeWorks) { index, s ->
                        AnimatedVisibility(
                            visible = true,
                            enter = MutantMotion.SetEnterTransition,
                            exit = MutantMotion.SetExitTransition
                        ) {
                            SetRowCard(
                                set = s,
                                displayIndex = index + 1,
                                executionQuality = executionQuality,
                                onDelete = { viewModel.deleteSet(s.id) }
                            )
                        }
                    }
                }

                // 5. Post-set Transient Feedback (Collapses in ~180ms after user selection)
                if (showPostSetFeedback) {
                    item {
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
                }

                // 6. NEXT SET Input Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_logger_card"),
                        shape = MutantShapeTokens.Panel,
                        color = MutantSurfaceCard,
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(MutantSpacing.md)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NEXT SET",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MutantVolt,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )

                                // Set Type Switcher
                                Row(
                                    modifier = Modifier
                                        .clip(MutantShapeTokens.TinyControl)
                                        .background(MutantDarkNavy)
                                        .border(1.dp, MutantBorder, MutantShapeTokens.TinyControl)
                                ) {
                                    SetTypeButton(
                                        label = "Warm-up",
                                        selected = selectedSetType == SetType.WARMUP,
                                        onClick = { selectedSetType = SetType.WARMUP }
                                    )
                                    SetTypeButton(
                                        label = "Work set",
                                        selected = selectedSetType == SetType.WORK,
                                        onClick = { selectedSetType = SetType.WORK }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))

                            // Weight & Reps Inputs with Steppers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Weight Input
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "PESO (KG)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MutantTextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                    OutlinedTextField(
                                        value = weightInput,
                                        onValueChange = { weightInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MutantTextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MutantVolt,
                                            unfocusedBorderColor = MutantBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = MutantSpacing.xxs),
                                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)
                                    ) {
                                        listOf(-5f, -2.5f, 2.5f, 5f).forEach { step ->
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val current = weightInput.toFloatOrNull() ?: 60f
                                                        val updated = (current + step).coerceAtLeast(0f)
                                                        weightInput = if (updated % 1f == 0f) updated.toInt().toString() else "%.1f".format(updated)
                                                    },
                                                shape = MutantShapeTokens.Compact,
                                                color = MutantDarkNavy
                                            ) {
                                                Text(
                                                    text = if (step > 0) "+$step" else "$step",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MutantTextSecondary,
                                                        fontSize = MutantTypeScale.micro
                                                    ),
                                                    modifier = Modifier.padding(vertical = MutantSpacing.xxs),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                // Reps Input
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "REPS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MutantTextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                    OutlinedTextField(
                                        value = repsInput,
                                        onValueChange = { repsInput = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MutantTextPrimary,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MutantVolt,
                                            unfocusedBorderColor = MutantBorder
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = MutantSpacing.xxs),
                                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)
                                    ) {
                                        listOf(-2, -1, 1, 2).forEach { step ->
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        val current = repsInput.toIntOrNull() ?: 10
                                                        repsInput = (current + step).coerceAtLeast(1).toString()
                                                    },
                                                shape = MutantShapeTokens.Compact,
                                                color = MutantDarkNavy
                                            ) {
                                                Text(
                                                    text = if (step > 0) "+$step" else "$step",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MutantTextSecondary,
                                                        fontSize = MutantTypeScale.micro
                                                    ),
                                                    modifier = Modifier.padding(vertical = MutantSpacing.xxs),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Barbell Plate Visualizer
                            val parsedWeight = weightInput.toFloatOrNull() ?: 60f
                            Spacer(modifier = Modifier.height(MutantSpacing.compact))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Plates: ${(parsedWeight - 20f).coerceAtLeast(0f) / 2f}kg / side",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                                )
                                PlateVisualizer(totalWeightKg = parsedWeight)
                            }

                            Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))

                            // RIR Selector [0] [1] [2] [3] [4+] (Smooth color transition +1dp elevation)
                            Text(
                                text = "RIR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(MutantSpacing.compact))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                            ) {
                                listOf(0, 1, 2, 3, 4).forEach { rirVal ->
                                    val isSelected = selectedRir == rirVal
                                    val rirBg by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (isSelected) (if (rirVal == 0) MutantRed else MutantVolt).copy(alpha = 0.25f) else MutantDarkNavy,
                                        animationSpec = tween(MotionDuration.Feedback),
                                        label = "RirBg"
                                    )
                                    val rirBorder by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (isSelected) (if (rirVal == 0) MutantRed else MutantVolt) else MutantBorder,
                                        animationSpec = tween(MotionDuration.Feedback),
                                        label = "RirBorder"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .offset(y = if (isSelected) (-1).dp else 0.dp)
                                            .clickable { selectedRir = rirVal },
                                        shape = MutantShapeTokens.TinyControl,
                                        color = rirBg,
                                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, rirBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (rirVal == 0) "0 (FALHA)" else if (rirVal == 4) "4+" else "$rirVal",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isSelected) (if (rirVal == 0) MutantRed else MutantVolt) else MutantTextSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))

                            // Technique Chips
                            Text(
                                text = "Technique",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(MutantSpacing.compact))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                            ) {
                                listOf(
                                    IntensityTechnique.NONE to "None",
                                    IntensityTechnique.REST_PAUSE to "Rest-pause",
                                    IntensityTechnique.DROP_SET to "Drop",
                                    IntensityTechnique.PARTIAL_REPS to "Partials"
                                ).forEach { (tech, label) ->
                                    val isSelected = selectedTechnique == tech
                                    val techBg by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (isSelected) MutantAmber.copy(alpha = 0.25f) else MutantDarkNavy,
                                        animationSpec = tween(MotionDuration.Feedback),
                                        label = "TechBg"
                                    )
                                    val techBorder by androidx.compose.animation.animateColorAsState(
                                        targetValue = if (isSelected) MutantAmber else MutantBorder,
                                        animationSpec = tween(MotionDuration.Feedback),
                                        label = "TechBorder"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp)
                                            .clickable {
                                                selectedTechnique = tech
                                                if (tech == IntensityTechnique.REST_PAUSE || tech == IntensityTechnique.DROP_SET) {
                                                    showIntensityDialog = true
                                                }
                                            },
                                        shape = MutantShapeTokens.TinyControl,
                                        color = techBg,
                                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, techBorder)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MutantAmber else MutantTextSecondary,
                                                    fontSize = MutantTypeScale.label
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Attached segments expansion
                            AnimatedVisibility(
                                visible = attachedSegments.isNotEmpty(),
                                enter = MutantMotion.CollapsibleEnterTransition,
                                exit = MutantMotion.CollapsibleExitTransition
                            ) {
                                Column(modifier = Modifier.padding(top = MutantSpacing.xs)) {
                                    Text(
                                        text = "Attached: ${attachedSegments.size} mini-segments configured",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MutantAmber
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(MutantSpacing.md))

                            // [ LOG SET ] Action with subtle 120ms compression feedback
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isLogButtonPressed = true
                                        delay(MotionDuration.Feedback.toLong())
                                        isLogButtonPressed = false

                                        val weight = weightInput.toFloatOrNull() ?: 60f
                                        val reps = repsInput.toIntOrNull() ?: 10
                                        viewModel.logSet(
                                            workoutExerciseId = activeExDetail.workoutExercise.id,
                                            type = selectedSetType,
                                            weightKg = weight,
                                            reps = reps,
                                            rir = selectedRir,
                                            technique = selectedTechnique,
                                            segments = attachedSegments,
                                            defaultRestSeconds = activeEx.defaultRestSeconds,
                                            muscleGroup = activeEx.muscleGroup
                                        )
                                        if (selectedSetType == SetType.WORK) {
                                            lastSavedSetSummary = "${if (weight % 1f == 0f) weight.toInt() else weight} kg × $reps @ $selectedRir saved"
                                            showPostSetFeedback = true
                                        }
                                        attachedSegments = emptyList()
                                        selectedTechnique = IntensityTechnique.NONE
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .scale(logButtonScale)
                                    .testTag("log_set_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MutantVolt,
                                    contentColor = MutantOnVolt
                                ),
                                shape = MutantShapeTokens.CompactControl
                            ) {
                                Text(
                                    text = "LOG SET",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )
                            }
                        }
                    }
                }

                // 7. Exercise Transition Controls
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MutantSpacing.xxs),
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
                    ) {
                        if (state.currentExerciseIndex > 0) {
                            OutlinedButton(
                                onClick = { viewModel.setCurrentExerciseIndex(state.currentExerciseIndex - 1) },
                                modifier = Modifier.weight(1f),
                                shape = MutantShapeTokens.CompactControl,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantTextSecondary),
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                            ) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(MutantSpacing.xxs))
                                Text("Previous")
                            }
                        }

                        if (state.currentExerciseIndex < state.exercises.size - 1) {
                            val nextExName = state.exercises.getOrNull(state.currentExerciseIndex + 1)?.exercise?.baseName?.ifEmpty {
                                state.exercises.getOrNull(state.currentExerciseIndex + 1)?.exercise?.name?.take(16)
                            } ?: "Next"
                            Button(
                                onClick = { viewModel.setCurrentExerciseIndex(state.currentExerciseIndex + 1) },
                                modifier = Modifier.weight(1.5f),
                                shape = MutantShapeTokens.CompactControl,
                                colors = ButtonDefaults.buttonColors(containerColor = MutantSurfaceElevated, contentColor = MutantVolt)
                            ) {
                                Text("Next: $nextExName", fontWeight = FontWeight.Bold, maxLines = 1)
                                Spacer(modifier = Modifier.width(MutantSpacing.xxs))
                                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                enabled = !isDiscarding,
                            onClick = { showFinishConfirmation = true },
                                modifier = Modifier.weight(1.5f),
                                shape = MutantShapeTokens.CompactControl,
                                colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                            ) {
                                Text("COMPLETE SESSION", fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !isDiscarding,
                            onClick = { viewModel.clearDiscardError(); showDiscardConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("discard_workout")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Discard workout")
                        }
                        Button(
                            onClick = { showFinishConfirmation = true }, enabled = !isDiscarding,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Finish workout")
                        }
                    }
                }
            }
        }
    }

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
                        viewModel.finishWorkout(workoutNotes, bw)
                        showFinishConfirmation = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                ) {
                    Text("SAVE & AUDIT", fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) {
                    Text("Cancel", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }
}

@Composable
private fun SetTypeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .padding(MutantSpacing.micro),
        shape = MutantShapeTokens.SmallControl,
        color = if (selected) MutantVolt else Color.Transparent
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                color = if (selected) MutantOnVolt else MutantTextSecondary
            ),
            modifier = Modifier.padding(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.compact)
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
