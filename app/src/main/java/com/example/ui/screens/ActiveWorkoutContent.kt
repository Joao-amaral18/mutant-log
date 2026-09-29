package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.designsystem.*
import com.example.ui.designsystem.components.MutantWorkoutSetLogger
import com.example.ui.viewmodel.ActiveWorkoutUiState
import java.util.Locale

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
    onDiscard: () -> Unit,
    onSelectExercise: (Int) -> Unit,
    onSetup: () -> Unit,
    onCues: () -> Unit,
    onTarget: () -> Unit,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRirChange: (Int) -> Unit,
    onTechniqueChange: (IntensityTechnique) -> Unit,
    onEditSegments: () -> Unit,
    onLogSet: () -> Unit,
    modifier: Modifier = Modifier,
    showFeedback: Boolean = false,
    setupEditor: @Composable () -> Unit = {},
    feedback: @Composable () -> Unit = {},
    recordedSets: @Composable () -> Unit = {},
    restTimer: @Composable () -> Unit = {}
) {
    val session = state.session ?: return
    val detail = state.exercises.getOrNull(state.currentExerciseIndex) ?: return
    val exercise = detail.exercise
    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(), containerColor = colors.background,
        // The activity already applies system-bar insets to its content.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().padding(horizontal = MutantSpacing.md, vertical = MutantSpacing.xs)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                    FilledIconButton(
                        onClick = onBack, modifier = Modifier.size(48.dp).testTag("workout_back"),
                        shape = MutantShapeTokens.InputChip,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.surface, contentColor = colors.onSurface)
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to home") }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)) {
                            Box(Modifier.size(6.dp).background(colors.secondary, CircleShape))
                            Text(session.title.uppercase(), style = WorkoutLoggerTokens.Label, color = colors.secondary,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("EXERCISE ${state.currentExerciseIndex + 1} OF ${state.exercises.size}",
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
                    }
                    OutlinedButton(onClick = onFinish, enabled = enabled,
                        shape = MutantShapeTokens.TinyControl, contentPadding = PaddingValues(horizontal = MutantSpacing.xs),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("finish_workout"),
                        border = BorderStroke(1.dp, colors.outline)) {
                        Text("END WORKOUT", style = WorkoutLoggerTokens.Label, color = colors.onSurface)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)) {
                    state.exercises.forEachIndexed { index, item ->
                        val completed = item.sets.count { it.setType == SetType.WORK } >= item.exercise.defaultWorkSets
                        val color = when { index == state.currentExerciseIndex -> colors.secondary; completed -> MutantColors.Success; else -> colors.outlineVariant }
                        TextButton(onClick = { onSelectExercise(index) }, enabled = enabled,
                            modifier = Modifier.weight(1f).height(32.dp).testTag("exercise_progress_$index"),
                            contentPadding = PaddingValues(0.dp)) {
                            Box(Modifier.fillMaxWidth().height(5.dp).background(color, CircleShape))
                        }
                    }
                }
            }
        },
        bottomBar = restTimer
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val screenPadding = if (maxWidth < 360.dp) MutantSpacing.sm else MutantSpacing.md
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("active_workout_content"),
                contentPadding = PaddingValues(start = screenPadding, end = screenPadding, top = MutantSpacing.xxs, bottom = MutantSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
            ) {
                item(key = "exercise_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)) {
                        Text(exercise.baseName.ifBlank { exercise.name }.uppercase(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = WorkoutLoggerTokens.DisplayFont,
                                fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp), color = colors.onSurface)
                        if (exercise.manufacturer.isNotBlank()) Text(exercise.manufacturer.uppercase(),
                            style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                            AssistChip(onClick = onSetup, enabled = enabled,
                                label = { Text("Seat: ${seatPosition.ifBlank { "Set up" }}", style = WorkoutLoggerTokens.Label) },
                                leadingIcon = { Icon(Icons.Default.Tune, null, Modifier.size(14.dp)) }, modifier = Modifier.testTag("edit_seat_setup"))
                            AssistChip(onClick = onSetup, enabled = enabled,
                                label = { Text(handlePosition.ifBlank { "Grip setup" }, style = WorkoutLoggerTokens.Label) },
                                modifier = Modifier.testTag("edit_handle_setup"))
                            AssistChip(onClick = onSetup, enabled = enabled,
                                label = { Text("Notes", style = WorkoutLoggerTokens.Label) },
                                leadingIcon = { Icon(Icons.Default.EditNote, null, Modifier.size(14.dp)) },
                                modifier = Modifier.testTag("exercise_notes"))
                            if (exercise.executionCues.isNotBlank()) AssistChip(onClick = onCues,
                                label = { Text("Cues", style = WorkoutLoggerTokens.Label) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, null, Modifier.size(14.dp)) }, modifier = Modifier.testTag("exercise_cues"))
                        }
                        setupEditor()
                    }
                }
                item(key = "target") {
                    Surface(onClick = onTarget, modifier = Modifier.fillMaxWidth().testTag("target_banner"),
                        color = colors.surface, shape = MutantShapeTokens.InputChip, border = BorderStroke(1.dp, colors.outlineVariant)) {
                        Row(Modifier.padding(MutantSpacing.sm), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text("PRESCRIBED TARGET", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                                Text("${exercise.defaultWorkSets} SETS × ${exercise.defaultRepMin}–${exercise.defaultRepMax} REPS @ ${exercise.defaultRir} RIR",
                                    style = WorkoutLoggerTokens.Label.copy(letterSpacing = 0.sp), color = colors.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(if (state.currentProgression?.status == ProgressionStatus.FIRST_TIME) "BASELINE" else "TARGET LOAD",
                                    style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                                val load = state.currentProgression?.suggestedWeightKg ?: detail.workoutExercise.nextSetWeightKg
                                Text(load?.let { String.format(Locale.ROOT, "%.1f KG", it) } ?: "SET LOAD",
                                    style = WorkoutLoggerTokens.Label, color = colors.secondary)
                            }
                        }
                    }
                }
                item(key = "logger") {
                    MutantWorkoutSetLogger(
                        exercise = exercise, workSetCount = detail.sets.count { it.setType == SetType.WORK },
                        warmupSetCount = detail.sets.count { it.setType == SetType.WARMUP },
                        weightValue = weightValue, repsValue = repsValue, setType = setType, rir = rir, technique = technique,
                        segments = segments, enabled = enabled, onWeightChange = onWeightChange, onRepsChange = onRepsChange,
                        onSetTypeChange = onSetTypeChange, onRirChange = onRirChange, onTechniqueChange = onTechniqueChange,
                        onEditSegments = onEditSegments, onLogSet = onLogSet
                    )
                }
                if (showFeedback) item(key = "feedback") { feedback() }
                if (detail.sets.isNotEmpty()) item(key = "recorded_sets") { recordedSets() }
                if (state.currentProgression?.historyWorkSets?.isNotEmpty() == true) {
                    item(key = "previous_performance") {
                        Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)) {
                            Text("LAST TIME", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                            state.currentProgression.historyWorkSets.forEach {
                                Text("${it.weightKg.formatLoad()} KG × ${it.reps} @ ${it.rir} RIR", style = WorkoutLoggerTokens.Label, color = colors.onSurface)
                            }
                        }
                    }
                }
                if (state.currentExerciseIndex < state.exercises.lastIndex) {
                    item(key = "next_exercise") {
                        val next = state.exercises[state.currentExerciseIndex + 1].exercise
                        Surface(onClick = { onSelectExercise(state.currentExerciseIndex + 1) }, enabled = enabled,
                            modifier = Modifier.fillMaxWidth().testTag("next_exercise"), shape = MutantShapeTokens.InputChip,
                            color = colors.surface, border = BorderStroke(1.dp, colors.outlineVariant)) {
                            Row(Modifier.padding(MutantSpacing.sm), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)) {
                                Text((state.currentExerciseIndex + 2).toString().padStart(2, '0'), style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                                Column(Modifier.weight(1f)) {
                                    Text("UP NEXT", style = WorkoutLoggerTokens.Label, color = colors.secondary)
                                    Text(next.baseName.ifBlank { next.name }.uppercase(), style = MaterialTheme.typography.labelMedium,
                                        color = colors.onSurface, fontWeight = FontWeight.Bold)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    item(key = "complete_workout") {
                        OutlinedButton(onClick = onFinish, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag("complete_session")) {
                            Text("COMPLETE SESSION", style = WorkoutLoggerTokens.Label, color = colors.secondary)
                        }
                    }
                }
                item(key = "discard") {
                    TextButton(onClick = onDiscard, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("discard_workout")) {
                        Icon(Icons.Default.DeleteOutline, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(MutantSpacing.xs))
                        Text("DISCARD WORKOUT SESSION", style = WorkoutLoggerTokens.Label, color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
