package com.example.ui.screens

import com.example.data.db.plannedSets
import com.example.data.db.isAdHoc
import com.example.data.db.isComplete
import com.example.data.db.sessionSets
import com.example.data.db.workSetCount
import com.example.data.db.repMin
import com.example.data.db.repMax
import com.example.data.db.plannedRir
import com.example.data.db.plannedRestSeconds
import com.example.data.db.WorkoutExerciseDetail
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.ActiveWorkoutUiState
import java.math.BigDecimal

fun loadLabel(value: Float): String = BigDecimal(value.toString()).stripTrailingZeros().toPlainString()

/** "8 kg", or "pin 8" for a pin-loaded stack. */
fun loadText(value: Float, stack: Boolean): String = if (stack) "pin ${loadLabel(value)}" else "${loadLabel(value)} kg"

fun restLabel(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

/** Plates per side on a 20 kg bar, heaviest first. */
fun platesPerSide(totalKg: Float): String {
    var perSide = ((totalKg - 20f) / 2f).toDouble()
    if (perSide <= 1e-6) return "Empty bar (20 kg)"
    val plates = mutableListOf<String>()
    listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25).forEach { plate ->
        while (perSide >= plate - 1e-9) {
            plates += loadLabel(plate.toFloat())
            perSide -= plate
        }
    }
    return if (plates.isEmpty()) "Empty bar (20 kg)" else "Plates per side: ${plates.joinToString(" + ")} kg · 20 kg bar"
}

/** Load and reps to aim for today, from the double-progression recommendation. */
fun targetFor(progression: ProgressionRecommendation?, previous: List<WorkoutSet>, detail: WorkoutExerciseDetail): Pair<Float, Int>? {
    if (progression == null || progression.status == ProgressionStatus.FIRST_TIME) return null
    val reps = when (progression.status) {
        ProgressionStatus.INCREASE_LOAD -> detail.repMin
        else -> ((previous.minOfOrNull { it.reps } ?: detail.repMin) + 1).coerceIn(detail.repMin, detail.repMax)
    }
    return progression.suggestedWeightKg to reps
}

/** Target for each planned set: +1 rep on each previous set until the top of the range, reset after a load increase. */
fun perSetTargets(progression: ProgressionRecommendation?, previous: List<WorkoutSet>, detail: WorkoutExerciseDetail): List<Pair<Float, Int>> {
    val target = targetFor(progression, previous, detail) ?: return emptyList()
    return (0 until detail.plannedSets).map { index ->
        val reps = if (progression?.status == ProgressionStatus.INCREASE_LOAD) detail.repMin
        else ((previous.getOrNull(index)?.reps ?: (target.second - 1)) + 1).coerceIn(detail.repMin, detail.repMax)
        target.first to reps
    }
}

private fun techniqueBadge(technique: IntensityTechnique): String? = when (technique) {
    IntensityTechnique.DROP_SET -> "DROP"
    IntensityTechnique.REST_PAUSE -> "R-P"
    IntensityTechnique.PARTIAL_REPS -> "PART"
    IntensityTechnique.ASSISTED_REPS -> "ASST"
    IntensityTechnique.NONE -> null
}

private val SetColumns = listOf(36.dp, 0.dp, 64.dp, 48.dp, 40.dp, 28.dp)

/** Presentation only: the screen coordinator keeps mutations and durable state in Room. */
@Composable
fun ActiveWorkoutContent(
    state: ActiveWorkoutUiState,
    weightValue: String,
    repsValue: String,
    setType: SetType,
    rir: Int,
    technique: IntensityTechnique,
    seatPosition: String,
    handlePosition: String,
    segments: List<SetSegment>,
    enabled: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onSetup: () -> Unit,
    onTarget: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRirChange: (Int) -> Unit,
    onTechniqueChange: (IntensityTechnique) -> Unit,
    onEditSegments: () -> Unit,
    onLogSet: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveSet: (Long) -> Unit = {},
    onRemovePlannedSet: () -> Unit = {},
    onAddSet: () -> Unit = {},
    onOpenList: () -> Unit = {},
    // Read lazily in the header label so the per-second tick does not recompose the session screen.
    elapsedSeconds: () -> Long = { 0L },
    // Shown instead of the running clock for a workout logged after the fact.
    pastWorkoutLabel: String? = null,
    onLoadUnitChange: (String) -> Unit = {},
    setupEditor: @Composable () -> Unit = {},
    restTimer: @Composable () -> Unit = {}
) {
    val session = state.session ?: return
    val detail = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
    val exercise = detail.exercise
    val workSets = detail.sets.filter { it.setType == SetType.WORK }
    val warmups = detail.sets.filter { it.setType == SetType.WARMUP }
    val doneSets = state.exercises.sumOf { d -> d.sets.count { it.setType == SetType.WORK } }
    val totalSets = state.exercises.sumOf { it.sessionSets }.coerceAtLeast(1)
    val adHoc = detail.isAdHoc
    val last = state.currentExerciseIndex >= state.exercises.lastIndex
    val nextDetail = state.exercises.getOrNull(state.currentExerciseIndex + 1)
    val nextLabel = nextDetail?.let { "Next: ${it.exercise.baseName.ifBlank { it.exercise.name }}" } ?: "Finish workout"
    val onNext = { if (nextDetail == null) onFinish() else onSelectExercise(state.currentExerciseIndex + 1) }

    var setupOpen by remember(detail.workoutExercise.id) { mutableStateOf(false) }
    val complete = detail.isComplete
    val showLogger = !complete || setType == SetType.WARMUP

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        containerColor = MutantColors.Background,
        // The activity already applies system-bar insets to its content.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().background(MutantColors.Background)) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp).testTag("workout_back")) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Back to Protocol",
                            tint = MutantColors.TextSecondary, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(session.title, style = MutantType.Title.copy(lineHeight = 20.sp), color = MutantColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (pastWorkoutLabel != null) Text("$pastWorkoutLabel · $doneSets/$totalSets sets",
                            style = MutantType.MonoLabel, color = MutantColors.Warning, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("past_workout_label"))
                        else TickingText({ "${formatClock(elapsedSeconds())} · $doneSets/$totalSets sets" },
                            style = MutantType.MonoLabel, color = MutantColors.TextSecondary)
                    }
                    Surface(
                        onClick = onFinish, enabled = enabled,
                        modifier = Modifier.size(44.dp).testTag("finish_workout"),
                        shape = CircleShape,
                        color = Color.Transparent,
                        contentColor = if (enabled) MutantColors.Primary else MutantColors.TextMetadata,
                        border = BorderStroke(1.dp, if (enabled) MutantColors.Primary else MutantColors.Line)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.SportsScore, contentDescription = "Finish session", modifier = Modifier.size(24.dp))
                        }
                    }
                }
                ProgressTrack(doneSets / totalSets.toFloat(), Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp))
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = onOpenList,
                        modifier = Modifier.height(36.dp).testTag("view_all_exercises"),
                        shape = RoundedCornerShape(18.dp),
                        color = MutantColors.SurfaceContainerHigh,
                        border = BorderStroke(1.dp, MutantColors.Outline)
                    ) {
                        Row(Modifier.padding(start = 10.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = null, tint = MutantColors.Primary,
                                modifier = Modifier.size(18.dp))
                            Text("View all", style = MutantType.Chip, color = MutantColors.TextPrimary, maxLines = 1)
                        }
                    }
                    Box(Modifier.width(1.dp).height(20.dp).background(MutantColors.Line))
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(end = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.exercises.forEachIndexed { index, item ->
                        ExercisePill(
                            number = index + 1,
                            label = item.exercise.baseName.ifBlank { item.exercise.name },
                            current = index == state.currentExerciseIndex,
                            finished = item.isComplete,
                            enabled = enabled,
                            onClick = { onSelectExercise(index) },
                            modifier = Modifier.testTag("exercise_progress_$index")
                        )
                    }
                }
                }
            }
        },
        bottomBar = restTimer
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).testTag("active_workout_content"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "exercise_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val source = exercise.manufacturer.ifBlank { exercise.muscleGroup }
                    MutantEyebrow("${state.currentExerciseIndex + 1}/${state.exercises.size} · ${source.uppercase()}",
                        style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.06.em))
                    Text(exercise.baseName.ifBlank { exercise.name }, style = MutantType.DisplayMedium, color = MutantColors.TextPrimary,
                        modifier = Modifier.testTag("exercise_name"))
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (adHoc) {
                            Text("Added today · no plan", style = MutantType.Chip, color = MutantColors.Primary,
                                modifier = Modifier.height(26.dp).background(MutantColors.PrimarySelected, RoundedCornerShape(8.dp))
                                    .wrapContentHeight().padding(horizontal = 10.dp).testTag("ad_hoc_chip"))
                        } else {
                            SpecChip("${detail.plannedSets} × ${detail.repMin}–${detail.repMax}")
                            SpecChip("RIR ${detail.plannedRir}")
                        }
                        SpecChip(restLabel(detail.plannedRestSeconds), withTimer = true)
                    }
                }
            }
            item(key = "target") {
                val target = if (adHoc) null else targetFor(state.currentProgression, state.previousWorkSets, detail)
                Surface(
                    onClick = onTarget,
                    modifier = Modifier.fillMaxWidth().testTag("target_banner"),
                    shape = RoundedCornerShape(18.dp),
                    color = MutantColors.PrimaryTint,
                    border = BorderStroke(1.dp, MutantColors.PrimaryTintBorder)
                ) {
                    Row(Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MutantEyebrow("TODAY’S TARGET", color = MutantColors.Primary)
                            Text(
                                target?.let { "${loadText(it.first, exercise.usesStack)} × ${it.second}" } ?: if (adHoc) "Log as you go" else "Set a baseline",
                                style = MutantType.MonoValue, color = MutantColors.TextPrimary
                            )
                            Text(
                                if (state.previousWorkSets.isEmpty()) "First time on this exercise"
                                else "Last time: " + state.previousWorkSets.joinToString(" · ") { "${loadLabel(it.weightKg)}×${it.reps}" },
                                style = MutantType.Caption.copy(lineHeight = 16.sp), color = MutantColors.TextSecondary
                            )
                        }
                        Icon(Icons.Outlined.Info, contentDescription = "How the target works", tint = MutantColors.Primary,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }
            item(key = "setup") {
                SetupCard(
                    seat = seatPosition, handle = handlePosition,
                    note = detail.workoutExercise.notes.ifBlank { exercise.notes },
                    cue = exercise.executionCues,
                    open = setupOpen, enabled = enabled,
                    onToggle = { setupOpen = !setupOpen }, onEdit = onSetup,
                    stack = exercise.usesStack, onLoadUnitChange = onLoadUnitChange,
                    editor = setupEditor
                )
            }
            item(key = "sets") {
                SetTable(
                    warmups = warmups, workSets = workSets, previous = state.previousWorkSets,
                    plannedSets = detail.plannedSets, adHoc = adHoc, stack = exercise.usesStack,
                    targets = perSetTargets(state.currentProgression, state.previousWorkSets, detail),
                    draftActive = showLogger && setType == SetType.WORK,
                    draftWeight = weightValue, draftReps = repsValue, draftRir = rir,
                    enabled = enabled, onRemoveSet = onRemoveSet, onRemovePlannedSet = onRemovePlannedSet, onAddSet = onAddSet
                )
            }
            if (showLogger) {
                item(key = "logger") {
                    SetLogger(
                        exercise = exercise, plannedSets = detail.plannedSets, workSetCount = workSets.size,
                        weightValue = weightValue, repsValue = repsValue, setType = setType, rir = rir,
                        technique = technique, segments = segments, enabled = enabled,
                        onWeightChange = onWeightChange, onRepsChange = onRepsChange,
                        onSetTypeChange = onSetTypeChange, onRirChange = onRirChange,
                        onTechniqueChange = onTechniqueChange, onEditSegments = onEditSegments, onLogSet = onLogSet
                    )
                }
                if (adHoc && workSets.isNotEmpty()) {
                    item(key = "ad_hoc_done") {
                        MutantButton("Done · $nextLabel", onClick = onNext, enabled = enabled, style = MutantButtonStyle.Outline,
                            height = 52.dp, textStyle = MutantType.Button.copy(fontSize = 15.sp),
                            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                            modifier = Modifier.fillMaxWidth().testTag("ad_hoc_next"))
                    }
                }
            } else {
                item(key = "exercise_done") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MutantColors.Success, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Exercise done", style = MutantType.ButtonSmall, color = MutantColors.Success)
                        }
                        if (nextDetail != null) UpNextCard(nextDetail, state.nextProgression, state.nextPreviousWorkSets)
                        MutantButton(nextLabel, onClick = onNext, enabled = enabled,
                            trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                            modifier = Modifier.fillMaxWidth().testTag(if (last) "complete_session" else "next_exercise"))
                    }
                }
            }
        }
    }
}

/** What comes after the finished exercise, so the user can walk over and set up while resting. */
@Composable
private fun UpNextCard(next: WorkoutExerciseDetail, progression: ProgressionRecommendation?, previous: List<WorkoutSet>) {
    val exercise = next.exercise
    val source = exercise.manufacturer.ifBlank { exercise.muscleGroup }
    val seat = next.workoutExercise.seatPosition.ifBlank { exercise.seatPosition }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(18.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(18.dp))
            .padding(16.dp)
            .testTag("up_next_card"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MutantEyebrow(if (source.isBlank()) "UP NEXT" else "UP NEXT · ${source.uppercase()}")
            Text(exercise.baseName.ifBlank { exercise.name }, style = MutantType.Title.copy(fontSize = 19.sp, lineHeight = 22.sp),
                color = MutantColors.TextPrimary)
        }
        val target = targetFor(progression, previous, next)
        val stats = listOf(
            "TARGET" to when {
                next.isAdHoc -> "Open"
                target != null -> "${loadLabel(target.first)} × ${target.second}"
                else -> "Baseline"
            },
            "SETS" to if (next.isAdHoc) "Open" else "${next.plannedSets} × ${next.repMin}–${next.repMax}",
            "SETUP" to (seat.takeIf { it.isNotBlank() }?.let { if (it.startsWith("seat", ignoreCase = true)) it else "Seat $it" } ?: "—")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.forEach { (label, value) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    MutantEyebrow(label, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
                    Text(value, style = MutantType.MonoBody.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = MutantColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Text(
            "Last time: " + if (previous.isEmpty()) "first time" else previous.joinToString(" · ") { "${loadLabel(it.weightKg)}×${it.reps}" },
            style = MutantType.Caption, color = MutantColors.TextSecondary
        )
    }
}

@Composable
private fun ExercisePill(number: Int, label: String, current: Boolean, finished: Boolean, enabled: Boolean,
                         onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick, enabled = enabled,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (current) MutantColors.PrimarySelected else MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, if (current) MutantColors.Primary else MutantColors.OutlineVariant)
    ) {
        Row(Modifier.padding(start = 8.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.sizeIn(minWidth = 20.dp, minHeight = 20.dp).background(
                    when {
                        finished -> MutantColors.Success.copy(alpha = 0.16f)
                        current -> MutantColors.Primary
                        else -> MutantColors.OutlineVariant
                    }, CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (finished) "✓" else "$number", style = MutantType.MonoLabel.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = when {
                        finished -> MutantColors.Success
                        current -> MutantColors.OnPrimary
                        else -> MutantColors.TextSecondary
                    })
            }
            Text(label, style = MutantType.Chip, maxLines = 1,
                color = if (finished && !current) MutantColors.TextSecondary else MutantColors.TextPrimary)
        }
    }
}

@Composable
private fun SpecChip(text: String, withTimer: Boolean = false) {
    Row(
        Modifier.height(26.dp).background(MutantColors.SurfaceContainerHigh, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (withTimer) Icon(Icons.Outlined.Timer, contentDescription = "Rest", tint = MutantColors.TextSecondary, modifier = Modifier.size(15.dp))
        Text(text, style = MutantType.MonoLabel.copy(fontSize = 12.sp), color = MutantColors.TextPrimary)
    }
}

@Composable
private fun SetupCard(
    seat: String, handle: String, note: String, cue: String,
    open: Boolean, enabled: Boolean,
    onToggle: () -> Unit, onEdit: () -> Unit,
    stack: Boolean, onLoadUnitChange: (String) -> Unit,
    editor: @Composable () -> Unit
) {
    val summary = listOfNotNull(
        seat.takeIf { it.isNotBlank() }?.let { if (it.startsWith("seat", ignoreCase = true)) it else "Seat $it" },
        handle.takeIf { it.isNotBlank() }?.let { if (it.contains("grip", ignoreCase = true)) it else "$it grip" }
    ).joinToString(" · ").ifBlank { "No setup saved" }
    Column(
        Modifier
            .fillMaxWidth()
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(18.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(18.dp))
    ) {
        Surface(onClick = onToggle, color = Color.Transparent, shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().testTag("exercise_setup_toggle")) {
            Row(Modifier.padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = MutantColors.TextSecondary, modifier = Modifier.size(20.dp))
                Text(summary, style = MutantType.BodySmall.copy(fontWeight = FontWeight.Medium), color = MutantColors.TextPrimary,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (open) "Hide setup" else "Show setup", tint = MutantColors.TextMetadata, modifier = Modifier.size(20.dp))
            }
        }
        AnimatedVisibility(open, enter = MutantMotion.CollapsibleEnterTransition, exit = MutantMotion.CollapsibleExitTransition) {
            Column(Modifier.padding(start = 48.dp, end = 16.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabeledLine("Setup:", note.ifBlank { "No notes yet." })
                if (cue.isNotBlank()) LabeledLine("Cue:", cue)
                TextButton(onClick = onEdit, enabled = enabled, contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp).testTag("edit_seat_setup")) {
                    Text("Edit setup", style = MutantType.ButtonSmall, color = MutantColors.Primary)
                }
                // Pin-loaded stacks are logged by position; their numbers are not kilograms.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Load in", style = MutantType.BodySmall, color = MutantColors.TextSecondary)
                    MutantChoiceChip("kg", !stack, { onLoadUnitChange(LOAD_UNIT_KG) }, enabled = enabled,
                        modifier = Modifier.testTag("load_unit_kg"))
                    MutantChoiceChip("Stack pin", stack, { onLoadUnitChange(LOAD_UNIT_STACK) }, enabled = enabled,
                        modifier = Modifier.testTag("load_unit_stack"))
                }
                editor()
            }
        }
    }
}

@Composable
private fun LabeledLine(label: String, text: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = MutantColors.TextPrimary)) { append(label) }
            append(" ")
            append(text)
        },
        style = MutantType.BodySmall.copy(lineHeight = 19.sp), color = MutantColors.TextSecondary
    )
}

@Composable
private fun SetTable(
    warmups: List<WorkoutSet>,
    workSets: List<WorkoutSet>,
    previous: List<WorkoutSet>,
    plannedSets: Int,
    adHoc: Boolean,
    stack: Boolean,
    targets: List<Pair<Float, Int>>,
    draftActive: Boolean,
    draftWeight: String,
    draftReps: String,
    draftRir: Int,
    enabled: Boolean,
    onRemoveSet: (Long) -> Unit,
    onRemovePlannedSet: () -> Unit,
    onAddSet: () -> Unit
) {
    var revealed by remember { mutableStateOf<String?>(null) }
    // Unlogged planned sets can be dropped for today, but never below one set or below what is logged.
    val canTrim = enabled && !adHoc && plannedSets > maxOf(1, workSets.size)
    @Composable
    fun Removable(key: String, onRemove: (() -> Unit)?, content: @Composable () -> Unit) {
        if (onRemove == null) {
            content()
            return
        }
        SwipeToRemoveRow(
            revealed = revealed == key, enabled = enabled,
            onReveal = { revealed = if (it) key else null },
            onRemove = { revealed = null; onRemove() },
            revealWidth = 76.dp, removeDistance = 150.dp, cornerRadius = 12.dp, compact = true,
            removeTag = "remove_$key"
        ) { content() }
    }
    Column(Modifier.fillMaxWidth().testTag("set_table"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SetGridRow(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp)) { col ->
            val header = listOf("SET", "LAST", if (stack) "PIN" else "KG", "REPS", "RIR", "")[col]
            if (header.isNotEmpty()) MutantEyebrow(header, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
        }
        warmups.forEach { set ->
            val remove = { onRemoveSet(set.id) }.takeIf { enabled }
            Removable("set_row_${set.id}", remove) {
                SetLine(label = "A", labelColor = MutantColors.TextSecondary, labelBackground = MutantColors.SurfaceContainerHigh,
                    previous = "—", kg = loadLabel(set.weightKg), reps = "${set.reps}", rir = "—",
                    textColor = MutantColors.TextSecondary, background = MutantColors.Background,
                    badge = null, onDelete = remove, tag = "set_row_${set.id}")
            }
        }
        val rows = when {
            adHoc -> workSets.size + if (draftActive) 1 else 0
            else -> maxOf(plannedSets, workSets.size)
        }
        for (index in 0 until rows) {
            val prev = previous.getOrNull(index)?.let { "${loadLabel(it.weightKg)}×${it.reps}" } ?: "—"
            val set = workSets.getOrNull(index)
            when {
                set != null -> {
                    val remove = { onRemoveSet(set.id) }.takeIf { enabled }
                    Removable("set_row_${set.id}", remove) {
                        SetLine(
                            label = "${index + 1}", labelColor = MutantColors.Success, labelBackground = MutantColors.Success.copy(alpha = 0.14f),
                            previous = prev, kg = loadLabel(set.weightKg), reps = "${set.reps}", rir = "${set.rir}",
                            textColor = MutantColors.TextPrimary, background = MutantColors.Background,
                            badge = when {
                                set.isPr -> Triple("PR", MutantColors.Warning.copy(alpha = 0.16f), MutantColors.Warning)
                                else -> techniqueBadge(set.technique)?.let { Triple(it, MutantColors.Line, MutantColors.TextSecondary) }
                            },
                            onDelete = remove, tag = "set_row_${set.id}"
                        )
                    }
                }
                index == workSets.size && draftActive -> Removable("set_row_current", onRemovePlannedSet.takeIf { canTrim }) {
                    SetLine(
                        label = "${index + 1}", labelColor = MutantColors.OnPrimary, labelBackground = MutantColors.Primary,
                        previous = prev, kg = draftWeight.toFloatOrNull()?.let(::loadLabel) ?: draftWeight.ifBlank { "—" }, reps = draftReps.ifBlank { "—" },
                        rir = if (draftRir >= 4) "4+" else "$draftRir",
                        textColor = MutantColors.TextPrimary, background = MutantColors.PrimaryRow,
                        badge = null, onDelete = null, tag = "set_row_current"
                    )
                }
                else -> Removable("set_row_pending_$index", onRemovePlannedSet.takeIf { canTrim }) {
                    SetLine(
                        label = "${index + 1}", labelColor = MutantColors.TextMetadata, labelBackground = MutantColors.SurfaceContainerHigh,
                        previous = prev,
                        // Upcoming sets show their target, dimmed, so the plan is readable at a glance.
                        kg = targets.getOrNull(index)?.let { loadLabel(it.first) } ?: "—",
                        reps = targets.getOrNull(index)?.second?.toString() ?: "—",
                        rir = "—",
                        textColor = MutantColors.TextMetadata, background = MutantColors.Background,
                        badge = null, onDelete = null, tag = "set_row_pending_$index"
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (adHoc) {
                Text("Log as many sets as you need", style = MutantType.Caption.copy(fontSize = 11.sp), color = MutantColors.TextMetadata,
                    modifier = Modifier.weight(1f))
            } else {
                TextButton(onClick = onAddSet, enabled = enabled, contentPadding = PaddingValues(start = 0.dp, end = 10.dp),
                    modifier = Modifier.height(40.dp).testTag("add_set")) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = MutantColors.Primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add set", style = MutantType.ButtonSmall.copy(fontWeight = FontWeight.SemiBold), color = MutantColors.Primary)
                }
                Spacer(Modifier.weight(1f))
            }
            Text("Swipe a set left to remove", style = MutantType.Caption.copy(fontSize = 11.sp), color = MutantColors.TextMetadata,
                textAlign = TextAlign.End)
        }
    }
}

@Composable
private fun SetGridRow(modifier: Modifier = Modifier, cell: @Composable (Int) -> Unit) {
    // Fixed columns grow with the system font size so large text never clips.
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SetColumns.forEachIndexed { col, width ->
            Box(
                if (width == 0.dp) Modifier.weight(1f) else Modifier.width(width * scale),
                contentAlignment = if (col in 2..4) Alignment.CenterEnd else Alignment.CenterStart
            ) { cell(col) }
        }
    }
}

@Composable
private fun SetLine(
    label: String, labelColor: Color, labelBackground: Color,
    previous: String, kg: String, reps: String, rir: String,
    textColor: Color, background: Color,
    badge: Triple<String, Color, Color>?,
    onDelete: (() -> Unit)?,
    tag: String
) {
    val value = MutantType.MonoBody.copy(textAlign = TextAlign.End)
    SetGridRow(
        Modifier
            .heightIn(min = 44.dp)
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 4.dp)
            .testTag(tag)
    ) { col ->
        when (col) {
            0 -> Box(Modifier.size(26.dp).background(labelBackground, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(label, style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Bold), color = labelColor)
            }
            1 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(previous, style = MutantType.MonoBody.copy(fontSize = 12.sp), color = MutantColors.TextMetadata, maxLines = 1)
                if (badge != null) {
                    Text(badge.first, style = MutantType.MonoLabel.copy(fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold),
                        color = badge.third,
                        modifier = Modifier.background(badge.second, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 3.dp))
                }
            }
            2 -> Text(kg, style = value, color = textColor, maxLines = 1)
            3 -> Text(reps, style = value, color = textColor, maxLines = 1)
            4 -> Text(rir, style = value, color = textColor, maxLines = 1)
            else -> if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("delete_$tag")) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove set", tint = MutantColors.TextMetadata, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SetLogger(
    exercise: Exercise,
    plannedSets: Int,
    workSetCount: Int,
    weightValue: String,
    repsValue: String,
    setType: SetType,
    rir: Int,
    technique: IntensityTechnique,
    segments: List<SetSegment>,
    enabled: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRirChange: (Int) -> Unit,
    onTechniqueChange: (IntensityTechnique) -> Unit,
    onEditSegments: () -> Unit,
    onLogSet: () -> Unit
) {
    val weight = weightValue.toFloatOrNull()
    val reps = repsValue.toIntOrNull()
    val valid = weight != null && weight.isFinite() && weight >= 0f && reps != null && reps > 0
    val stack = exercise.usesStack
    val step = if (stack) 1f else exercise.defaultIncrementKg.takeIf { it.isFinite() && it > 0f } ?: 2.5f
    val warmup = setType == SetType.WARMUP
    Column(
        Modifier
            .fillMaxWidth()
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(22.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(22.dp))
            .padding(14.dp)
            .testTag("set_logger_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val pickTechnique: (IntensityTechnique) -> Unit = {
                if (warmup) onSetTypeChange(SetType.WORK)
                onTechniqueChange(it)
            }
            MutantChoiceChip("Straight", !warmup && technique == IntensityTechnique.NONE, { pickTechnique(IntensityTechnique.NONE) },
                enabled = enabled, modifier = Modifier.testTag("technique_NONE"))
            MutantChoiceChip("Warm-up", warmup, {
                if (technique != IntensityTechnique.NONE) onTechniqueChange(IntensityTechnique.NONE)
                onSetTypeChange(SetType.WARMUP)
            }, enabled = enabled, modifier = Modifier.testTag("set_type_WARMUP"))
            listOf(
                IntensityTechnique.DROP_SET to "Drop set",
                IntensityTechnique.REST_PAUSE to "Rest-pause",
                IntensityTechnique.PARTIAL_REPS to "Partials"
            ).forEach { (option, label) ->
                MutantChoiceChip(label, !warmup && technique == option, { pickTechnique(option) },
                    enabled = enabled, modifier = Modifier.testTag("technique_${option.name}"))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MutantStepper(
                label = if (stack) "LOAD · STACK PIN" else "LOAD · KG", value = weightValue, onValueChange = onWeightChange,
                onMinus = { onWeightChange(loadLabel(((weight ?: 0f) - step).coerceAtLeast(0f))) },
                onPlus = { onWeightChange(loadLabel((weight ?: 0f) + step)) },
                tag = "weight", valueDescription = if (stack) "Load as stack pin position" else "Load in kilograms", enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            MutantStepper(
                label = "REPS", value = repsValue, onValueChange = onRepsChange,
                onMinus = { onRepsChange(((reps ?: 1) - 1).coerceAtLeast(1).toString()) },
                onPlus = { onRepsChange(((reps ?: 0) + 1).coerceAtMost(999).toString()) },
                tag = "reps", allowDecimal = false, valueDescription = "Repetitions", enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MutantEyebrow("RIR · REPS IN RESERVE", modifier = Modifier.padding(start = 4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0..4).forEach { option ->
                    MutantChoiceChip(
                        label = if (option == 4) "4+" else "$option",
                        selected = if (option == 4) rir >= 4 else rir == option,
                        onClick = { onRirChange(option) },
                        enabled = enabled && !warmup,
                        modifier = Modifier.weight(1f).testTag("rir_option_$option"),
                        height = 44.dp, cornerRadius = 12.dp, horizontalPadding = 0.dp,
                        textStyle = MutantType.MonoBody.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        if (technique == IntensityTechnique.REST_PAUSE || technique == IntensityTechnique.DROP_SET) {
            MutantButton(
                if (segments.isEmpty()) "Add segments" else "Edit ${segments.size} segments · " +
                    segments.joinToString(" · ") { "${loadLabel(it.weightKg)}×${it.reps}" },
                onClick = onEditSegments, enabled = enabled, style = MutantButtonStyle.Outline, height = 44.dp,
                textStyle = MutantType.ButtonSmall, modifier = Modifier.fillMaxWidth().testTag("edit_set_segments")
            )
        }
        MutantButton(
            when {
                warmup -> "Log warm-up"
                plannedSets == 0 -> "Log set ${workSetCount + 1}"
                else -> "Log set ${workSetCount + 1}/$plannedSets"
            },
            onClick = onLogSet, enabled = enabled && valid, icon = Icons.Rounded.Check,
            modifier = Modifier.fillMaxWidth().testTag("log_set_button")
        )
        if (!valid) {
            Text("Enter a load of 0 kg or more and at least 1 rep.", style = MutantType.Caption, color = MutantColors.Error,
                modifier = Modifier.testTag("set_input_error"))
        } else {
            Text(if (stack) "Stack position, not kg" else platesPerSide(weight ?: 0f), style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Normal),
                color = MutantColors.TextMetadata, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
