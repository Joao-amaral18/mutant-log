package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.designsystem.*
import com.example.ui.designsystem.components.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.HistoryUiState
import com.example.ui.viewmodel.MutantViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: MutantViewModel, onNavigateBack: () -> Unit) {
    val state by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val editError by viewModel.editError.collectAsStateWithLifecycle()
    val editing by viewModel.isLoadingWorkout.collectAsStateWithLifecycle()
    HistoryContent(state, onNavigateBack, viewModel::reloadHistory, viewModel::editWorkout, editError, editing)
}

private sealed interface HistoryView {
    data object Main : HistoryView
    data class Session(val workout: HistoryWorkout) : HistoryView
    data class Exercise(val summary: HistoryExerciseSummary) : HistoryView
}

@Composable
internal fun HistoryContent(
    state: HistoryUiState,
    onNavigateBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEdit: (Long) -> Unit = {},
    editError: String? = null,
    editing: Boolean = false
) {
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var sessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var exerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selectedSession = state.workouts.firstOrNull { it.session.id == sessionId }
    val summaries = remember(state.workouts) { WorkoutHistory.exercises(state.workouts) }
    val selectedExercise = summaries.firstOrNull { it.exercise.id == exerciseId }
    BackHandler(sessionId != null || exerciseId != null) {
        if (sessionId != null) sessionId = null else exerciseId = null
    }
    val currentView = when {
        selectedSession != null -> HistoryView.Session(selectedSession)
        selectedExercise != null -> HistoryView.Exercise(selectedExercise)
        else -> HistoryView.Main
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MutantColors.Background, contentColor = MutantColors.TextPrimary) {
        androidx.compose.animation.AnimatedContent(
            targetState = currentView,
            transitionSpec = {
                val isBack = (initialState is HistoryView.Session && targetState is HistoryView.Exercise) ||
                        (targetState is HistoryView.Main)
                if (isBack) {
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    )) togetherWith (androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ))
                } else {
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    ) + androidx.compose.animation.fadeIn(
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    )) togetherWith (androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it / 4 },
                        animationSpec = androidx.compose.animation.core.tween(MutantMotion.Navigation)
                    ) + androidx.compose.animation.fadeOut(
                        animationSpec = androidx.compose.animation.core.tween(120)
                    ))
                }
            },
            label = "HistoryViewTransition"
        ) { view ->
            when (view) {
                is HistoryView.Session -> HistorySessionDetail(
                    workout = view.workout,
                    all = state.workouts,
                    onBack = { sessionId = null },
                    onEdit = onEdit,
                    editError = editError,
                    editing = editing
                )
                is HistoryView.Exercise -> HistoryExerciseDetail(
                    summary = view.summary,
                    onBack = { exerciseId = null },
                    onSession = { sessionId = it }
                )
                is HistoryView.Main -> Column(Modifier.fillMaxSize().testTag("history_screen")) {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("History", style = MutantType.ScreenTitle, color = MutantColors.TextPrimary)
                        HistorySegments(listOf("Workouts", "Exercises", "Calendar"), mode) { mode = it }
                    }
                    when {
                        state.loading -> com.example.ui.designsystem.components.MutantLoadingScreen(
                            message = "Loading history…",
                            modifier = Modifier.fillMaxSize().testTag("history_loading")
                        )
                        state.error != null -> Column(Modifier.padding(MutantSpacing.lg)) {
                            Text(state.error, color = MutantColors.Error)
                            TextButton(onClick = onRetry) { Text("Try again") }
                        }
                        else -> androidx.compose.animation.AnimatedContent(
                            targetState = mode,
                            transitionSpec = { MutantMotion.ScreenFadeThroughSpec },
                            label = "HistoryTabTransition"
                        ) { currentMode ->
                            when (currentMode) {
                                0 -> HistoryTimeline(state.workouts) { sessionId = it }
                                1 -> HistoryExerciseList(summaries) { exerciseId = it }
                                else -> HistoryCalendar(state.workouts) { sessionId = it }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistoryHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(end = MutantSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun HistoryTimeline(workouts: List<HistoryWorkout>, onOpen: (Long) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var types by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var days by rememberSaveable { mutableIntStateOf(0) }
    var prs by rememberSaveable { mutableStateOf(false) }
    val recordedTitles = remember(workouts) { WorkoutHistory.titles(workouts) }
    val selectedTypes = types.filter { it in recordedTitles }.toSet()
    val filtered = remember(workouts, query, types, days, prs) { WorkoutHistory.filter(workouts, query, selectedTypes, days.takeIf { it > 0 }, prs) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val month = historyMonth(monthOffset)
    val periodWorkouts = filtered.filter { historyDate(it.session.startedAt, "yyyy-MM") == historyDate(month.timeInMillis, "yyyy-MM") }
    val searching = query.isNotBlank() || selectedTypes.isNotEmpty() || days > 0 || prs
    val visible = if (searching) filtered else periodWorkouts
    val now = Calendar.getInstance()
    val weekStart = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, -((get(Calendar.DAY_OF_WEEK) + 5) % 7))
    }.timeInMillis
    val groups = visible.groupBy { workout ->
        val date = historyDate(workout.session.startedAt, "yyyy-MM-dd")
        when {
            date == historyDate(now.timeInMillis, "yyyy-MM-dd") -> "TODAY"
            date == historyDate((now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis, "yyyy-MM-dd") -> "YESTERDAY"
            workout.session.startedAt >= weekStart && workout.session.startedAt <= now.timeInMillis -> "THIS WEEK"
            else -> historyDate(workout.session.startedAt, "MMMM yyyy").uppercase(historyLocale)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("history_timeline"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { HistorySearch(query, { query = it }, "Search workouts", "history_search") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { MutantChoiceChip("All", selectedTypes.isEmpty() && days == 0 && !prs, { types = arrayListOf(); days = 0; prs = false }) }
                items(recordedTitles, key = { it }) { type ->
                    MutantChoiceChip(type, type in selectedTypes, { types = ArrayList(if (type in selectedTypes) selectedTypes - type else selectedTypes + type) }, modifier = Modifier.testTag("history_filter_$type"))
                }
                items(listOf(7, 30)) { count -> MutantChoiceChip("$count days", days == count, { days = if (days == count) 0 else count }) }
                item { MutantChoiceChip("PRs only", prs, { prs = !prs }) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searching) MutantEyebrow("SEARCH RESULTS", style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em))
                else HistoryMonthSelector(month, { monthOffset-- }, { monthOffset++ }, monthOffset < 0)
                HistoryMetrics(listOf("WORKOUTS" to visible.size.toString(), "TIME" to historyDuration(visible.sumOf { it.minutes }), "SETS" to visible.sumOf { it.sets.size }.toString(), "VOLUME" to "${historyNumber(visible.sumOf { it.volume })} kg"))
            }
        }
        if (visible.isEmpty()) item { HistoryEmpty(if (workouts.isEmpty()) "Your history starts with your next workout." else if (searching) "No workouts match these filters." else "No workouts this month.", if (workouts.isEmpty()) "Finish a session to track your progress here." else if (searching) "Change the search or filters to see other workouts." else "Go back a month to find earlier sessions.") }
        groups.forEach { (label, sessions) ->
            item(key = "group-$label") { MutantEyebrow(label, modifier = Modifier.padding(start = 4.dp, top = 10.dp), style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em)) }
            items(sessions, key = { it.session.id }) { HistorySessionCard(it) { onOpen(it.session.id) } }
        }
        if (!searching && workouts.any { historyDate(it.session.startedAt, "yyyy-MM") < historyDate(month.timeInMillis, "yyyy-MM") }) item {
            MutantButton("Previous month", onClick = { monthOffset-- }, style = MutantButtonStyle.Surface, height = 48.dp,
                textStyle = MutantType.ButtonSmall, modifier = Modifier.fillMaxWidth().testTag("history_older"))
        }
    }
}

@Composable
internal fun HistorySegments(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(14.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .background(if (on) MutantColors.Line else Color.Transparent, RoundedCornerShape(10.dp))
                    .selectable(on, role = Role.Tab) { onSelect(index) }
                    .testTag("history_tab_$index"),
                contentAlignment = Alignment.Center
            ) { Text(label, style = MutantType.ButtonSmall, color = if (on) MutantColors.TextPrimary else MutantColors.TextSecondary) }
        }
    }
}

@Composable
internal fun HistorySessionCard(workout: HistoryWorkout, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MutantColors.SurfaceContainer,
        border = BorderStroke(1.dp, MutantColors.OutlineVariant),
        modifier = Modifier.fillMaxWidth().testTag("history_session_${workout.session.id}")
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(workout.session.title, style = MutantType.Title, color = MutantColors.TextPrimary)
                    Text("${historyDate(workout.session.startedAt, "EEE MMM d")} · ${historyDuration(workout.minutes)}",
                        style = MutantType.Caption, color = MutantColors.TextSecondary)
                }
                if (workout.prs > 0) HistoryPrBadge(workout.prs)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryCell("EXER.", "${workout.exercises.size}", Modifier.weight(1f))
                HistoryCell("SETS", "${workout.sets.size}", Modifier.weight(1f))
                HistoryCell("VOLUME", "${historyNumber(workout.volume)} kg", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        MutantEyebrow(label, style = MutantType.Eyebrow.copy(fontSize = 9.5.sp))
        Text(value, style = MutantType.MonoBody.copy(fontSize = 13.sp), color = MutantColors.TextPrimary, maxLines = 1)
    }
}

@Composable
internal fun HistorySearch(value: String, onChange: (String) -> Unit, label: String, tag: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(MutantColors.SurfaceContainer, RoundedCornerShape(14.dp))
            .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MutantColors.TextMetadata, modifier = Modifier.size(20.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MutantType.Body.copy(color = MutantColors.TextPrimary),
            cursorBrush = SolidColor(MutantColors.Primary),
            modifier = Modifier.weight(1f).testTag(tag),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(label, style = MutantType.Body, color = MutantColors.TextMetadata)
                inner()
            }
        )
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Close, "Clear search", tint = MutantColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun HistoryMetrics(metrics: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, value) ->
                    MutantStatWell(label.uppercase(Locale.ROOT), value, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun HistoryPrBadge(count: Int) {
    Text(
        "$count PR${if (count == 1) "" else "s"}",
        style = MutantType.MonoLabel.copy(fontWeight = FontWeight.Bold),
        color = MutantColors.Warning,
        modifier = Modifier.background(MutantColors.Warning.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
internal fun HistoryEmpty(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = MutantSpacing.xl), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MutantType.Title, color = MutantColors.TextPrimary)
        Text(description, style = MutantType.BodySmall, color = MutantColors.TextSecondary)
    }
}

internal val historyLocale: Locale = Locale.US
internal fun historyDate(timestamp: Long, pattern: String): String = SimpleDateFormat(pattern, historyLocale).format(Date(timestamp))
internal fun historyNumber(value: Number): String = NumberFormat.getNumberInstance(historyLocale).apply { maximumFractionDigits = 1 }.format(value)
internal fun historySetText(set: WorkoutSet): String = "${historyNumber(set.weightKg)} × ${set.reps}"
internal fun historyDuration(minutes: Int): String = if (minutes < 60) "${minutes}min" else "${minutes / 60}h ${"%02d".format(minutes % 60)}min"
internal fun historyMonth(offset: Int): Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
