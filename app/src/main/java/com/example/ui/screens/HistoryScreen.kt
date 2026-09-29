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
import com.example.ui.designsystem.components.MutantCard
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
                    HistoryHeader("Histórico", onNavigateBack)
                    TabRow(selectedTabIndex = mode) {
                        listOf("Treinos", "Exercícios", "Calendário").forEachIndexed { index, title ->
                            Tab(
                                selected = mode == index,
                                onClick = { mode = index },
                                text = { Text(title) },
                                modifier = Modifier.testTag("history_tab_$index")
                            )
                        }
                    }
                    when {
                        state.loading -> com.example.ui.designsystem.components.MutantLoadingScreen(
                            message = "Carregando histórico de treinos...",
                            modifier = Modifier.fillMaxSize().testTag("history_loading")
                        )
                        state.error != null -> Column(Modifier.padding(MutantSpacing.lg)) {
                            Text(state.error, color = MutantColors.Error)
                            TextButton(onClick = onRetry) { Text("Tentar novamente") }
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
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
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
            date == historyDate(now.timeInMillis, "yyyy-MM-dd") -> "HOJE"
            date == historyDate((now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis, "yyyy-MM-dd") -> "ONTEM"
            workout.session.startedAt >= weekStart && workout.session.startedAt <= now.timeInMillis -> "ESTA SEMANA"
            else -> historyDate(workout.session.startedAt, "MMMM yyyy").uppercase(historyLocale)
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("history_timeline"), contentPadding = PaddingValues(MutantSpacing.md), verticalArrangement = Arrangement.spacedBy(MutantSpacing.sm)) {
        item { HistorySearch(query, { query = it }, "Buscar no histórico", "history_search") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                item { FilterChip(selected = selectedTypes.isEmpty() && days == 0 && !prs, onClick = { types = arrayListOf(); days = 0; prs = false }, label = { Text("Todos") }) }
                items(recordedTitles, key = { it }) { type ->
                    FilterChip(selected = type in selectedTypes, onClick = { types = ArrayList(if (type in selectedTypes) selectedTypes - type else selectedTypes + type) }, label = { Text(type) }, modifier = Modifier.testTag("history_filter_$type"))
                }
                items(listOf(7, 30)) { count -> FilterChip(selected = days == count, onClick = { days = if (days == count) 0 else count }, label = { Text("$count dias") }) }
                item { FilterChip(selected = prs, onClick = { prs = !prs }, label = { Text("PRs") }) }
            }
        }
        item {
            if (searching) Text("RESULTADOS DA BUSCA E FILTROS", style = MaterialTheme.typography.labelLarge, color = MutantColors.TextSecondary)
            else HistoryMonthSelector(month, { monthOffset-- }, { monthOffset++ }, monthOffset < 0)
            HistoryMetrics(listOf("Treinos" to visible.size.toString(), "Duração" to historyDuration(visible.sumOf { it.minutes }), "Séries" to visible.sumOf { it.sets.size }.toString(), "Volume" to "${historyNumber(visible.sumOf { it.volume })} kg"))
        }
        if (visible.isEmpty()) item { HistoryEmpty(if (workouts.isEmpty()) "Seu histórico começa no próximo treino." else if (searching) "Nenhum treino corresponde aos filtros." else "Nenhum treino neste mês.", if (workouts.isEmpty()) "Conclua uma sessão para acompanhar sua evolução aqui." else if (searching) "Altere a busca ou os filtros para ver outros treinos." else "Navegue pelos meses para encontrar sessões anteriores.") }
        groups.forEach { (label, sessions) ->
            item(key = "group-$label") { Text(label, style = MaterialTheme.typography.labelLarge, color = MutantColors.TextSecondary, modifier = Modifier.padding(top = MutantSpacing.sm)) }
            items(sessions, key = { it.session.id }) { HistorySessionCard(it) { onOpen(it.session.id) } }
        }
        if (!searching && workouts.any { historyDate(it.session.startedAt, "yyyy-MM") < historyDate(month.timeInMillis, "yyyy-MM") }) item {
            OutlinedButton(onClick = { monthOffset-- }, modifier = Modifier.fillMaxWidth().testTag("history_older")) { Text("Ver mês anterior") }
        }
    }
}

@Composable
internal fun HistorySessionCard(workout: HistoryWorkout, onOpen: () -> Unit) {
    MutantCard(onClick = onOpen, testTag = "history_session_${workout.session.id}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FitnessCenter, null, tint = MutantColors.Primary)
            Spacer(Modifier.width(MutantSpacing.sm))
            Column(Modifier.weight(1f)) {
                Text(workout.session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${historyDate(workout.session.startedAt, "dd/MM/yyyy · HH:mm")} · ${historyDuration(workout.minutes)}", style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
            }
            if (workout.prs > 0) HistoryPrBadge(workout.prs)
        }
        Spacer(Modifier.height(MutantSpacing.sm))
        Text("${workout.exercises.size} exercícios · ${workout.sets.size} séries · ${historyNumber(workout.volume)} kg", style = MaterialTheme.typography.bodySmall)
        workout.exercises.take(2).forEach { ex ->
            Spacer(Modifier.height(MutantSpacing.sm))
            Text(ex.exercise.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            Text(ex.workSets.joinToString(" · ") { historySetText(it.set) }.ifEmpty { "Sem séries de trabalho" }, style = MaterialTheme.typography.bodySmall, color = MutantColors.TextSecondary)
        }
        Spacer(Modifier.height(MutantSpacing.xs))
        Text("Ver treino ›", style = MaterialTheme.typography.labelLarge, color = MutantColors.Primary)
    }
}

@Composable
internal fun HistorySearch(value: String, onChange: (String) -> Unit, label: String, tag: String) {
    OutlinedTextField(value, onChange, placeholder = { Text(label) }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = {
        if (value.isNotEmpty()) IconButton(onClick = { onChange("") }) { Icon(Icons.Default.Close, "Limpar busca") }
    }, singleLine = true, shape = MutantShapeTokens.CompactControl, modifier = Modifier.fillMaxWidth().testTag(tag))
}

@Composable
internal fun HistoryMetrics(metrics: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
        metrics.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)) {
                pair.forEach { (label, value) ->
                    Column(Modifier.weight(1f).padding(vertical = MutantSpacing.xs)) {
                        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MutantColors.TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistoryPrBadge(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.EmojiEvents, null, tint = MutantColors.Warning, modifier = Modifier.size(MutantSpacing.md))
        Text(" $count PR${if (count == 1) "" else "s"}", color = MutantColors.Warning, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun HistoryEmpty(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = MutantSpacing.xl), verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MutantColors.TextSecondary)
    }
}

internal val historyLocale = Locale.forLanguageTag("pt-BR")
internal fun historyDate(timestamp: Long, pattern: String): String = SimpleDateFormat(pattern, historyLocale).format(Date(timestamp))
internal fun historyNumber(value: Number): String = NumberFormat.getNumberInstance(historyLocale).apply { maximumFractionDigits = 1 }.format(value)
internal fun historySetText(set: WorkoutSet): String = "${historyNumber(set.weightKg)} × ${set.reps}"
internal fun historyDuration(minutes: Int): String = if (minutes < 60) "${minutes}min" else "${minutes / 60}h ${"%02d".format(minutes % 60)}min"
internal fun historyMonth(offset: Int): Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, offset) }
