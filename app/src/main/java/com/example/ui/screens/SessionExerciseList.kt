package com.example.ui.screens

import com.example.data.db.isAdHoc
import com.example.data.db.isComplete
import com.example.data.db.plannedSets
import com.example.data.db.sessionSets
import com.example.data.db.workSetCount
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.WorkoutExerciseDetail
import com.example.data.model.Exercise
import com.example.data.model.SetType
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.ActiveWorkoutUiState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RevealWidth = 96.dp
private val RemoveDistance = 170.dp

/** Every exercise in today's session: jump, swipe left to remove, or add one for today only. */
@Composable
fun SessionExerciseList(
    state: ActiveWorkoutUiState,
    enabled: Boolean,
    onBack: () -> Unit,
    onPick: (Int) -> Unit,
    onRemove: (WorkoutExerciseDetail) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    restTimer: @Composable () -> Unit = {}
) {
    val session = state.session ?: return
    val doneSets = state.exercises.sumOf { d -> d.sets.count { it.setType == SetType.WORK } }
    val totalSets = state.exercises.sumOf { it.sessionSets }
    var revealedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("session_exercise_list"),
        containerColor = MutantColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                Modifier.fillMaxWidth().background(MutantColors.Background).padding(start = 12.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp).testTag("session_list_back")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to exercise", tint = MutantColors.TextSecondary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("All exercises", style = MutantType.Title, color = MutantColors.TextPrimary)
                    Text("${session.title} · ${state.exercises.size} exercises · $doneSets/$totalSets sets",
                        style = MutantType.MonoLabel, color = MutantColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        bottomBar = restTimer
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "hint") {
                Row(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.TouchApp, contentDescription = null, tint = MutantColors.TextMetadata, modifier = Modifier.size(16.dp))
                    Text("Tap to jump · swipe left to remove", style = MutantType.MonoLabel, color = MutantColors.TextMetadata)
                }
            }
            itemsIndexed(state.exercises, key = { _, d -> d.workoutExercise.id }) { index, detail ->
                val id = detail.workoutExercise.id
                SwipeToRemoveRow(
                    revealed = revealedId == id,
                    enabled = enabled,
                    onReveal = { revealedId = if (it) id else null },
                    onRemove = { revealedId = null; onRemove(detail) },
                    modifier = Modifier.testTag("session_list_item_$index")
                ) {
                    SessionExerciseRow(
                        index = index, detail = detail, current = index == state.currentExerciseIndex,
                        enabled = enabled,
                        onClick = { if (revealedId != null) revealedId = null else onPick(index) },
                        onDelete = { revealedId = null; onRemove(detail) }
                    )
                }
            }
            item(key = "add") {
                val dash = MutantColors.Outline
                Surface(
                    onClick = onAdd, enabled = enabled,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(56.dp)
                        .drawBehind {
                            drawRoundRect(dash, cornerRadius = CornerRadius(18.dp.toPx()),
                                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                        }
                        .testTag("session_add_exercise")
                ) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MutantColors.Primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add exercise", style = MutantType.Button.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = MutantColors.Primary)
                    }
                }
            }
        }
    }
}

/** Swipe left to reveal Remove; a long swipe removes at once. Shared by exercise rows and set rows. */
@Composable
internal fun SwipeToRemoveRow(
    revealed: Boolean,
    enabled: Boolean,
    onReveal: (Boolean) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    revealWidth: Dp = RevealWidth,
    removeDistance: Dp = RemoveDistance,
    cornerRadius: Dp = 18.dp,
    compact: Boolean = false,
    removeTag: String = "session_list_remove",
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val reveal = with(density) { revealWidth.toPx() }
    val remove = with(density) { removeDistance.toPx() }
    val max = with(density) { (removeDistance + 50.dp).toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(cornerRadius)
    LaunchedEffect(revealed) {
        val target = if (revealed) -reveal else 0f
        if (offset.value != target && !offset.isRunning) offset.animateTo(target, tween(240))
    }
    Box(modifier.fillMaxWidth().clip(shape)) {
        if (offset.value < 0f) {
            Row(
                Modifier.matchParentSize().background(MutantColors.Destructive, shape),
                horizontalArrangement = Arrangement.End
            ) {
                val action = Modifier.width(revealWidth).fillMaxHeight().clickable(enabled = enabled, onClick = onRemove).testTag(removeTag)
                if (compact) {
                    Row(action, horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MutantColors.OnDestructive, modifier = Modifier.size(18.dp))
                        Text("Remove", style = MutantType.Chip, color = MutantColors.OnDestructive)
                    }
                } else {
                    Column(action, horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = MutantColors.OnDestructive, modifier = Modifier.size(22.dp))
                        Text("Remove", style = MutantType.Chip.copy(fontSize = 11.sp), color = MutantColors.OnDestructive)
                    }
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    state = rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-max, 0f)) }
                    },
                    onDragStopped = {
                        when {
                            offset.value < -remove -> {
                                offset.animateTo(0f, tween(240))
                                onReveal(false)
                                onRemove()
                            }
                            offset.value < -reveal / 2 -> { offset.animateTo(-reveal, tween(240)); onReveal(true) }
                            else -> { offset.animateTo(0f, tween(240)); onReveal(false) }
                        }
                    }
                )
        ) { content() }
    }
}

@Composable
private fun SessionExerciseRow(
    index: Int,
    detail: WorkoutExerciseDetail,
    current: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val exercise = detail.exercise
    val done = detail.workSetCount
    val planned = detail.plannedSets
    val adHoc = detail.isAdHoc
    val finished = detail.isComplete
    val started = done > 0
    val (status, statusColor) = when {
        current -> "CURRENT" to MutantColors.Primary
        finished -> "DONE" to MutantColors.Success
        started -> "IN PROGRESS" to MutantColors.Warning
        else -> "UP NEXT" to MutantColors.TextMetadata
    }
    Surface(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = if (current) MutantColors.PrimaryRow else MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, if (current) MutantColors.Primary else MutantColors.OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(32.dp).background(
                    when {
                        finished -> MutantColors.Success.copy(alpha = 0.16f)
                        current -> MutantColors.Primary
                        else -> MutantColors.OutlineVariant
                    }, CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (finished) "✓" else "${index + 1}", style = MutantType.MonoLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = when {
                        finished -> MutantColors.Success
                        current -> MutantColors.OnPrimary
                        else -> MutantColors.TextSecondary
                    })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(exercise.baseName.ifBlank { exercise.name }, style = MutantType.RowTitle.copy(fontSize = 15.sp),
                    color = MutantColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val source = exercise.manufacturer.ifBlank { exercise.muscleGroup }
                if (source.isNotBlank()) Text(source, style = MutantType.Caption.copy(fontSize = 11.5.sp), color = MutantColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.fillMaxWidth().height(3.dp).background(MutantColors.OutlineVariant, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(if (adHoc) (if (done > 0) 1f else 0f) else (done / planned.toFloat()).coerceIn(0f, 1f)).fillMaxHeight()
                        .background(if (finished) MutantColors.Success else MutantColors.Primary, RoundedCornerShape(2.dp)))
                }
            }
            Column(Modifier.widthIn(min = 76.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(if (adHoc) "$done set${if (done == 1) "" else "s"}" else "$done/$planned", style = MutantType.MonoBody.copy(fontWeight = FontWeight.Bold), color = MutantColors.TextPrimary)
                Text(status, style = MutantType.Chip.copy(fontSize = 10.5.sp), color = statusColor, maxLines = 1)
            }
            IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(40.dp).testTag("session_list_delete_$index")) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove ${exercise.baseName.ifBlank { exercise.name }}",
                    tint = MutantColors.TextMetadata, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Library search for a today-only addition. */
@Composable
fun AddSessionExerciseSheet(
    library: List<Exercise>,
    sessionExerciseIds: Set<Long>,
    currentLabel: String,
    onAdd: (exercise: Exercise, afterCurrent: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var afterCurrent by remember { mutableStateOf(true) }
    val matches = remember(library, sessionExerciseIds, query) {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        library.asSequence()
            .filter { it.id !in sessionExerciseIds }
            .filter { ex ->
                val text = "${ex.name} ${ex.baseName} ${ex.manufacturer} ${ex.muscleGroup}".lowercase()
                terms.all { it in text }
            }
            .take(40)
            .toList()
    }
    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("add_session_exercise_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Add exercise", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text("Today only — your plan stays the same.", style = MutantType.BodySmall, color = MutantColors.TextSecondary,
                modifier = Modifier.offset(y = (-4).dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MutantEyebrow("INSERT", modifier = Modifier.padding(start = 4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MutantColors.Background, MutantShapeTokens.Selector)
                        .border(1.dp, MutantColors.OutlineVariant, MutantShapeTokens.Selector)
                        .padding(4.dp)
                ) {
                    listOf(true to "After $currentLabel", false to "At the end").forEach { (value, label) ->
                        val on = afterCurrent == value
                        Box(
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(if (on) MutantColors.Line else androidx.compose.ui.graphics.Color.Transparent, MutantShapeTokens.CompactControl)
                                .clickable { afterCurrent = value }
                                .padding(horizontal = 8.dp)
                                .testTag(if (value) "add_position_after" else "add_position_end"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MutantType.ButtonSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = if (on) MutantColors.TextPrimary else MutantColors.TextSecondary)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(MutantColors.Background, MutantShapeTokens.Selector)
                    .border(1.dp, MutantColors.Line, MutantShapeTokens.Selector)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MutantColors.TextMetadata,
                    modifier = Modifier.size(20.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = MutantType.Body.copy(color = MutantColors.TextPrimary),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MutantColors.Primary),
                    modifier = Modifier.weight(1f).testTag("add_exercise_search"),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search exercise or machine", style = MutantType.Body, color = MutantColors.TextMetadata)
                        inner()
                    }
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                matches.forEach { ex ->
                    Surface(
                        onClick = { onAdd(ex, afterCurrent) },
                        shape = RoundedCornerShape(16.dp),
                        color = MutantColors.Background,
                        border = BorderStroke(1.dp, MutantColors.OutlineVariant),
                        modifier = Modifier.fillMaxWidth().testTag("add_exercise_option_${ex.id}")
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(ex.baseName.ifBlank { ex.name }, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                                Text(
                                    listOf(ex.muscleGroup, ex.manufacturer)
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MutantType.MonoLabel.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                                    color = MutantColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Rounded.AddCircle, contentDescription = "Add ${ex.baseName.ifBlank { ex.name }}",
                                tint = MutantColors.Primary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                if (matches.isEmpty()) {
                    Text("No match in your library.", style = MutantType.BodySmall, color = MutantColors.TextMetadata,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Composable
fun RemoveSessionExerciseSheet(name: String, loggedSets: Int, onKeep: () -> Unit, onRemove: () -> Unit) {
    MutantBottomSheet(onDismiss = onKeep, modifier = Modifier.testTag("remove_session_exercise_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Remove $name?", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text(
                "$loggedSets logged set${if (loggedSets == 1) "" else "s"} will be deleted from this session. Your plan stays the same.",
                style = MutantType.Body.copy(lineHeight = 20.sp), color = MutantColors.TextSecondary
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MutantButton("Keep", onClick = onKeep, style = MutantButtonStyle.Outline, height = 54.dp,
                    textStyle = MutantType.Button.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                MutantButton("Remove", onClick = onRemove, style = MutantButtonStyle.Danger, height = 54.dp,
                    textStyle = MutantType.Button.copy(fontSize = 15.sp),
                    modifier = Modifier.weight(1f).testTag("confirm_remove_session_exercise"))
            }
        }
    }
}
