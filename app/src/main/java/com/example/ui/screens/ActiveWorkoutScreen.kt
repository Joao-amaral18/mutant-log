package com.example.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.sessionSets
import com.example.data.db.repMin
import com.example.data.db.repMax
import com.example.data.db.plannedRir
import com.example.data.db.plannedRestSeconds
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.ReadinessInput
import com.example.ui.components.*
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.LocalMutantToast
import com.example.ui.designsystem.components.MutantButton
import com.example.ui.viewmodel.MutantViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SessionSheet { FINISH, DISCARD, PROGRESSION, READINESS, ADD_EXERCISE, REMOVE_EXERCISE }

@Composable
fun ActiveWorkoutScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    val state by viewModel.activeWorkoutUiState.collectAsStateWithLifecycle()
    // Not delegated: the value is read inside the clock label and rest bar only, so ticking stays local.
    val clock = viewModel.workoutClock.collectAsStateWithLifecycle()
    val isDiscarding by viewModel.isDiscardingWorkout.collectAsState()
    val isFinishing by viewModel.isFinishingWorkout.collectAsState()
    val discardError by viewModel.discardError.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val programDays by viewModel.programDays.collectAsState(initial = emptyList())
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val lastFinished by viewModel.lastFinishedWorkout.collectAsState(initial = null)
    val isStartingWorkout by viewModel.isStartingWorkout.collectAsState()
    val startWorkoutError by viewModel.startWorkoutError.collectAsState()
    val toast = LocalMutantToast.current

    var sheet by remember(state.session?.id) { mutableStateOf<SessionSheet?>(null) }
    var listOpen by remember(state.session?.id) { mutableStateOf(false) }
    var removeTarget by remember(state.session?.id) { mutableStateOf<com.example.data.db.WorkoutExerciseDetail?>(null) }
    var discardFromFinish by remember(state.session?.id) { mutableStateOf(false) }
    var showFreeSessionDialog by remember { mutableStateOf(false) }
    val session = state.session
    val loggedSets = state.exercises.sumOf { d -> d.sets.count { it.setType == SetType.WORK } }

    if (sheet == SessionSheet.DISCARD && session != null) {
        DiscardWorkoutDialog(
            isDiscarding = isDiscarding,
            error = discardError,
            loggedSets = loggedSets,
            onDismiss = { sheet = if (discardFromFinish) SessionSheet.FINISH else null },
            onDiscard = {
                viewModel.discardWorkout {
                    toast.show("Session discarded")
                    onNavigateBack()
                }
            }
        )
    }

    if (showFreeSessionDialog) {
        StartFreeSessionDialog(
            exercises = allExercises,
            isStarting = isStartingWorkout,
            error = startWorkoutError,
            onDismiss = { showFreeSessionDialog = false },
            onStart = { title, exerciseIds ->
                viewModel.startAdHocWorkout(title = title, exerciseIds = exerciseIds, readiness = ReadinessInput()) {
                    showFreeSessionDialog = false
                }
            }
        )
    }

    if (state.isLoading) {
        com.example.ui.designsystem.components.MutantLoadingScreen(
            modifier = modifier.testTag("active_workout_loading"),
            message = "Loading workout…",
            subMessage = "Restoring exercises and sets"
        )
        return
    }

    if (session == null || state.exercises.isEmpty()) {
        val plannedDay = programDays.find { it.id == systemStatus.recommendedProgramDayId }
            ?: programDays.firstOrNull { !it.isRestDay }
        if (sheet == SessionSheet.READINESS && plannedDay != null && session == null) {
            ReadinessDialog(
                workoutTitle = plannedDay.title,
                onDismiss = { sheet = null },
                onConfirm = { input ->
                    sheet = null
                    viewModel.startWorkout(plannedDay, input)
                }
            )
        }
        NoActiveWorkoutContent(
            lastSessionTitle = systemStatus.lastWorkoutTitle,
            lastSessionDaysAgo = systemStatus.lastWorkoutDaysAgo,
            hasEmptySession = session != null && state.exercises.isEmpty(),
            isDiscarding = isDiscarding,
            onNavigateToProtocols = onNavigateBack,
            onNavigateToHistory = onNavigateToHistory,
            onStartFreeSession = { showFreeSessionDialog = true },
            onDiscardWorkout = {
                viewModel.clearDiscardError()
                discardFromFinish = false
                sheet = SessionSheet.DISCARD
            },
            plannedWorkoutTitle = plannedDay?.title,
            onStartPlanned = plannedDay?.let { { sheet = SessionSheet.READINESS } },
            modifier = modifier
        )
        return
    }

    val currentExDetail = state.exercises.getOrNull(state.currentExerciseIndex) ?: state.exercises.first()
    val exercise = currentExDetail.exercise
    val workoutExercise = currentExDetail.workoutExercise
    val progression = state.currentProgression
    val coroutineScope = rememberCoroutineScope()

    // Transient input for the next set. The chosen load/reps are mirrored to Room as the next-set plan.
    var selectedSetType by remember(workoutExercise.id) { mutableStateOf(SetType.WORK) }
    var weightInput by remember(workoutExercise.id, progression?.suggestedWeightKg) {
        val initialWeight = workoutExercise.nextSetWeightKg
            ?: targetFor(progression, state.previousWorkSets, currentExDetail)?.first
            ?: progression?.suggestedWeightKg ?: 60f
        mutableStateOf(loadLabel(initialWeight))
    }
    var repsInput by remember(workoutExercise.id, progression?.suggestedRepsMin) {
        val initialReps = workoutExercise.nextSetReps
            ?: targetFor(progression, state.previousWorkSets, currentExDetail)?.second
            ?: progression?.suggestedRepsMin ?: currentExDetail.repMin
        mutableStateOf(initialReps.toString())
    }
    LaunchedEffect(workoutExercise.id, weightInput, repsInput) {
        // Wait for the target before persisting, or the placeholder would override it.
        if (progression == null && workoutExercise.nextSetWeightKg == null) return@LaunchedEffect
        val weight = weightInput.toFloatOrNull()
        val reps = repsInput.toIntOrNull()
        if (weight == null || !weight.isFinite() || weight < 0f || reps == null || reps <= 0) return@LaunchedEffect
        // Every write re-emits the session; skip unchanged values and let typing or stepper taps settle first.
        if (weight == workoutExercise.nextSetWeightKg && reps == workoutExercise.nextSetReps) return@LaunchedEffect
        kotlinx.coroutines.delay(NextSetSaveDelayMs)
        viewModel.updateNextSet(workoutExercise.id, weight, reps)
    }
    var selectedRir by remember(workoutExercise.id) { mutableIntStateOf(currentExDetail.plannedRir) }
    var selectedTechnique by remember(workoutExercise.id) { mutableStateOf(IntensityTechnique.NONE) }
    var attachedSegments by remember(workoutExercise.id) { mutableStateOf<List<SetSegment>>(emptyList()) }
    var isLogButtonPressed by remember { mutableStateOf(false) }
    var showIntensityDialog by remember(workoutExercise.id) { mutableStateOf(false) }
    var showSetupEditor by remember(workoutExercise.id) { mutableStateOf(false) }
    var seatPos by remember(workoutExercise.id, workoutExercise.seatPosition) { mutableStateOf(workoutExercise.seatPosition) }
    var handlePos by remember(workoutExercise.id, workoutExercise.handlePosition) { mutableStateOf(workoutExercise.handlePosition) }
    var exerciseNotes by remember(workoutExercise.id, workoutExercise.notes) { mutableStateOf(workoutExercise.notes) }

    val finishRequested by viewModel.finishRequested.collectAsState()
    LaunchedEffect(finishRequested, session.id) {
        if (finishRequested) {
            sheet = SessionSheet.FINISH
            viewModel.consumeFinishRequest()
        }
    }

    // Announce personal records as Room marks them.
    val prSetIds = state.exercises.flatMap { d -> d.sets.filter { it.isPr }.map { it.id to d.exercise } }
    var announcedPrs by remember(session.id) { mutableStateOf<Set<Long>?>(null) }
    LaunchedEffect(prSetIds) {
        val known = announcedPrs
        val fresh = prSetIds.filter { known != null && it.first !in known }
        fresh.lastOrNull()?.let { (_, ex) -> toast.show("New PR on ${ex.baseName.ifBlank { ex.name }}!") }
        announcedPrs = prSetIds.map { it.first }.toSet()
    }

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

    if (sheet == SessionSheet.PROGRESSION) {
        val target = targetFor(progression, state.previousWorkSets, currentExDetail)
        ProgressionSheet(
            targetText = target?.let { "${loadLabel(it.first)} kg × ${it.second}" } ?: "Set a baseline",
            repRange = "${currentExDetail.repMin}–${currentExDetail.repMax}",
            targetRir = currentExDetail.plannedRir,
            lastTimeText = state.previousWorkSets.takeIf { it.isNotEmpty() }
                ?.joinToString(" · ") { "${loadLabel(it.weightKg)}×${it.reps}" } ?: "No history",
            reason = progression?.reason,
            onDismiss = { sheet = null }
        )
    }

    if (sheet == SessionSheet.FINISH) {
        // Stack-pin loads are positions, not kilograms, so they stay out of the kg volume.
        val volume = state.exercises.filterNot { it.exercise.usesStack }.sumOf { d ->
            d.sets.filter { it.setType == SetType.WORK }.sumOf { (it.weightKg * it.reps).toDouble() }
        }.toFloat()
        FinishSessionSheet(
            title = session.title,
            elapsedMinutes = if (session.isRetroactive) session.durationMinutes.toLong() else viewModel.workoutClock.value.elapsedSeconds / 60,
            doneSets = loggedSets,
            totalSets = state.exercises.sumOf { it.sessionSets },
            volumeKg = volume,
            initialBodyweight = lastFinished?.bodyweight,
            lastBodyweightNote = lastFinished?.takeIf { it.bodyweight > 0f }?.let { last ->
                val days = com.example.data.model.WorkoutRecommendationEngine.localDaysBetween(
                    last.finishedAt ?: last.startedAt, System.currentTimeMillis(), java.util.TimeZone.getDefault())
                "Last logged ${loadLabel(last.bodyweight)} kg · ${relativeDays(days)}"
            },
            isFinishing = isFinishing,
            onSave = { notes, bodyweight ->
                viewModel.finishWorkout(notes, bodyweight, onError = { toast.show(it) }) {
                    sheet = null
                    toast.show("Workout saved · targets updated")
                    onNavigateToHistory()
                }
            },
            onDiscard = {
                viewModel.clearDiscardError()
                discardFromFinish = true
                sheet = SessionSheet.DISCARD
            },
            onDismiss = { sheet = null }
        )
    }

    val saveQuality: (String, String) -> Unit = { execution, target ->
        viewModel.updateExerciseDetails(workoutExercise.id, execution, target, seatPos, handlePos, workoutExercise.notes)
    }

    val restTimer: @Composable () -> Unit = {
            val rest = clock.value
            RestTimerBar(
                remainingSeconds = rest.restRemainingSeconds,
                isRunning = rest.restRunning,
                isComplete = rest.restCompleted,
                plannedSeconds = rest.restTotalSeconds.takeIf { it > 0 } ?: currentExDetail.plannedRestSeconds,
                executionQuality = workoutExercise.executionQuality,
                targetMuscleQuality = workoutExercise.targetMuscleQuality,
                onExecutionQuality = { saveQuality(it, workoutExercise.targetMuscleQuality) },
                onTargetMuscleQuality = { saveQuality(workoutExercise.executionQuality, it) },
                onAdjustTime = viewModel::adjustRestTimer,
                onTogglePlayPause = viewModel::toggleRestTimer,
                onSkip = viewModel::skipRestTimer
            )
        }

    val removeExercise: (com.example.data.db.WorkoutExerciseDetail) -> Unit = remove@{ detail ->
        if (state.exercises.size <= 1) {
            toast.show("Keep at least one exercise")
            return@remove
        }
        if (detail.sets.isNotEmpty()) {
            removeTarget = detail
            sheet = SessionSheet.REMOVE_EXERCISE
        } else {
            val name = detail.exercise.baseName.ifBlank { detail.exercise.name }
            viewModel.removeExerciseFromSession(detail.workoutExercise.id,
                onRemoved = { toast.show("$name removed", "Undo") { viewModel.undoRemoveExercise { toast.show(it) } } },
                onError = { toast.show(it) })
        }
    }

    if (sheet == SessionSheet.ADD_EXERCISE) {
        AddSessionExerciseSheet(
            library = allExercises,
            sessionExerciseIds = state.exercises.map { it.exercise.id }.toSet(),
            currentLabel = exercise.baseName.ifBlank { exercise.name },
            onAdd = { picked, afterCurrent ->
                sheet = null
                viewModel.addExerciseToSession(picked.id, afterCurrent,
                    onAdded = { toast.show("${picked.baseName.ifBlank { picked.name }} added") },
                    onError = { toast.show(it) })
            },
            onDismiss = { sheet = null }
        )
    }

    val pendingRemoval = removeTarget
    if (sheet == SessionSheet.REMOVE_EXERCISE && pendingRemoval != null) {
        val name = pendingRemoval.exercise.baseName.ifBlank { pendingRemoval.exercise.name }
        RemoveSessionExerciseSheet(
            name = name,
            loggedSets = pendingRemoval.sets.size,
            onKeep = { sheet = null; removeTarget = null },
            onRemove = {
                sheet = null
                removeTarget = null
                viewModel.removeExerciseFromSession(pendingRemoval.workoutExercise.id,
                    onRemoved = { toast.show("$name removed", "Undo") { viewModel.undoRemoveExercise { toast.show(it) } } },
                    onError = { toast.show(it) })
            }
        )
    }

    BackHandler(enabled = listOpen) { listOpen = false }
    if (listOpen) {
        SessionExerciseList(
            state = state,
            enabled = !isDiscarding && !isFinishing,
            onBack = { listOpen = false },
            onPick = { index -> listOpen = false; viewModel.setCurrentExerciseIndex(index) },
            onRemove = removeExercise,
            onAdd = { sheet = SessionSheet.ADD_EXERCISE },
            modifier = modifier,
            restTimer = restTimer
        )
        return
    }

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
        onFinish = { sheet = SessionSheet.FINISH },
        onSelectExercise = viewModel::setCurrentExerciseIndex,
        onSetup = { showSetupEditor = !showSetupEditor },
        onTarget = { sheet = SessionSheet.PROGRESSION },
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
        onRemoveSet = { setId ->
            viewModel.removeLoggedSet(setId,
                onRemoved = { toast.show("Set removed", "Undo") { viewModel.undoSetChange { toast.show(it) } } },
                onError = { toast.show(it) })
        },
        onRemovePlannedSet = {
            viewModel.removePlannedSet(workoutExercise.id,
                onRemoved = { toast.show("Set removed from today", "Undo") { viewModel.undoSetChange { toast.show(it) } } },
                onError = { toast.show(it) })
        },
        onAddSet = { viewModel.addPlannedSet(workoutExercise.id) { toast.show(it) } },
        onOpenList = { listOpen = true },
        elapsedSeconds = { clock.value.elapsedSeconds },
        pastWorkoutLabel = session?.takeIf { it.isRetroactive }?.let { pastWorkoutLabel(it) },
        onLoadUnitChange = { viewModel.setExerciseLoadUnit(exercise.id, it) },
        onLogSet = {
            val weight = weightInput.toFloatOrNull()
            val reps = repsInput.toIntOrNull()
            if (weight != null && weight.isFinite() && weight >= 0f && reps != null && reps > 0 && !isLogButtonPressed) {
                viewModel.logSet(
                    workoutExerciseId = workoutExercise.id,
                    type = selectedSetType, weightKg = weight, reps = reps, rir = selectedRir,
                    technique = selectedTechnique, segments = attachedSegments,
                    defaultRestSeconds = if (selectedSetType == SetType.WARMUP) 60 else currentExDetail.plannedRestSeconds,
                    muscleGroup = exercise.muscleGroup
                )
                attachedSegments = emptyList()
                selectedTechnique = IntensityTechnique.NONE
                selectedSetType = SetType.WORK
                isLogButtonPressed = true
                coroutineScope.launch {
                    delay(MutantMotion.Feedback.toLong())
                    isLogButtonPressed = false
                }
            }
        },
        modifier = modifier,
        setupEditor = {
            AnimatedVisibility(
                visible = showSetupEditor,
                enter = MutantMotion.CollapsibleEnterTransition,
                exit = MutantMotion.CollapsibleExitTransition
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MutantColors.Primary, unfocusedBorderColor = MutantColors.Line,
                        focusedLabelColor = MutantColors.Primary, unfocusedLabelColor = MutantColors.TextSecondary,
                        focusedTextColor = MutantColors.TextPrimary, unfocusedTextColor = MutantColors.TextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(seatPos, { seatPos = it }, label = { Text("Seat") }, singleLine = true,
                            colors = fieldColors, textStyle = MutantType.BodySmall, modifier = Modifier.weight(1f))
                        OutlinedTextField(handlePos, { handlePos = it }, label = { Text("Grip") }, singleLine = true,
                            colors = fieldColors, textStyle = MutantType.BodySmall, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(exerciseNotes, { exerciseNotes = it }, label = { Text("Setup notes") }, maxLines = 3,
                        colors = fieldColors, textStyle = MutantType.BodySmall,
                        modifier = Modifier.fillMaxWidth().testTag("exercise_notes_input"))
                    MutantButton(
                        "Save setup",
                        onClick = {
                            viewModel.updateExerciseDetails(workoutExercise.id, workoutExercise.executionQuality,
                                workoutExercise.targetMuscleQuality, seatPos, handlePos, exerciseNotes)
                            showSetupEditor = false
                        },
                        height = 44.dp, textStyle = MutantType.ButtonSmall,
                        modifier = Modifier.align(Alignment.End).testTag("save_setup")
                    )
                }
            }
        },
        restTimer = restTimer
    )
}

private const val NextSetSaveDelayMs = 400L

/** "Past · Sat 3 Oct, 08:30 · 60 min" for a workout being logged after the fact. */
internal fun pastWorkoutLabel(session: com.example.data.model.WorkoutSession): String =
    "Past · " + java.text.SimpleDateFormat("EEE d MMM, HH:mm", java.util.Locale.US).format(java.util.Date(session.startedAt)) +
        " · ${session.durationMinutes} min"

