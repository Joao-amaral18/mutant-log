package com.example.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.sessionSets
import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.R
import com.example.ui.components.ReadinessDialog
import com.example.ui.components.PastWorkoutSheet
import com.example.ui.components.SettingsDataDialog
import com.example.ui.components.GymPickerSheet
import com.example.ui.components.AddProgramExerciseSheet
import com.example.ui.components.CustomVariantSheet
import com.example.ui.components.CreateProgramSheet
import com.example.ui.components.gymTag
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.components.LocalMutantToast
import com.example.ui.designsystem.components.MutantTopBar
import com.example.ui.designsystem.components.MutantCard
import com.example.ui.designsystem.components.MutantCardVariant
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel

@Composable
fun HomeScreen(
    viewModel: MutantViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {}
) {
    val isStarting by viewModel.isStartingWorkout.collectAsState()
    val isAdoptingTemplate by viewModel.isAdoptingTemplate.collectAsState()
    val startError by viewModel.startWorkoutError.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()
    val programDays by viewModel.programDays.collectAsState(initial = emptyList())
    val selectedGym by viewModel.selectedGym.collectAsState()
    val allGyms by viewModel.allGyms.collectAsState(initial = emptyList())
    val activeSession by viewModel.activeWorkoutSession.collectAsState(initial = null)
    val activeUiState by viewModel.activeWorkoutUiState.collectAsState()
    // Read only inside the in-progress label, so the per-second tick stays out of the screen scope.
    val clock = viewModel.workoutClock.collectAsStateWithLifecycle()
    val libraryCounts by viewModel.libraryCounts.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val allVariants by viewModel.allVariants.collectAsState(initial = emptyList())
    val allMuscleGroups by viewModel.allMuscleGroups.collectAsState(initial = emptyList())

    var showReadinessDialog by remember { mutableStateOf(false) }
    var selectedDayToStart by remember { mutableStateOf<ProgramDay?>(null) }
    var showGymPicker by remember { mutableStateOf(false) }
    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCreateVariantDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPastWorkout by remember { mutableStateOf(false) }
    val activeProgram by viewModel.activeProgram.collectAsState(initial = null)
    val rotation = activeProgram?.isRotation == true
    var targetProgramDayId by remember { mutableStateOf<Long?>(null) }
    var selectedExerciseForVariant by remember { mutableStateOf<Exercise?>(null) }

    val recommendedProgramDay = programDays.find { it.id == systemStatus.recommendedProgramDayId }
        ?: programDays.firstOrNull { !it.isRestDay }

    val finishedWorkouts by viewModel.finishedWorkouts.collectAsState(initial = emptyList())
    val daySummaries by viewModel.programDaySummaries.collectAsState()
    val toast = LocalMutantToast.current
    val nowMillis = System.currentTimeMillis()

    if (showSettingsDialog) {
        SettingsDataDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showReadinessDialog && selectedDayToStart != null) {
        ReadinessDialog(
            workoutTitle = selectedDayToStart!!.title,
            onDismiss = { showReadinessDialog = false },
            onConfirm = { readinessInput ->
                showReadinessDialog = false
                viewModel.startWorkout(selectedDayToStart!!, readinessInput, onNavigateToActiveWorkout)
            }
        )
    }

    if (showGymPicker) {
        GymPickerSheet(
            gyms = allGyms,
            selectedGymId = selectedGym?.id,
            onSelect = { gym ->
                viewModel.selectGym(gym)
                showGymPicker = false
                toast.show("Gym: ${gym.name}")
            },
            onDismiss = { showGymPicker = false }
        )
    }

    val selectedGymIndex = allGyms.indexOfFirst { it.id == selectedGym?.id }.coerceAtLeast(0)
    val weekRows = if (rotation) buildRotationRows(programDays, finishedWorkouts, recommendedProgramDay?.id)
        else buildWeekRows(programDays, finishedWorkouts, recommendedProgramDay?.id, nowMillis)
    // Start always starts: the recommended day can already be done this week once the routine cycles.
    val startDay: (ProgramDay) -> Unit = { day ->
        if (activeSession != null) onNavigateToActiveWorkout()
        else {
            selectedDayToStart = day
            showReadinessDialog = true
        }
    }
    // Tapping a week row that is already done shows that workout in History instead.
    val openDay: (ProgramDay) -> Unit = { day ->
        if (weekRows.firstOrNull { it.day.id == day.id }?.status == WeekDayStatus.DONE) onNavigateToHistory() else startDay(day)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MutantColors.Background),
        contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
    ) {
        item(key = "top_bar") {
            MutantTopBar(
                gymName = selectedGym?.name,
                gymTag = gymTag(selectedGymIndex),
                onGymClick = { showGymPicker = true },
                onSettingsClick = { showSettingsDialog = true },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp)
            )
        }
        if (programDays.isEmpty()) {
            item(key = "empty_program") {
                EmptyProgramCard(
                    exerciseCount = libraryCounts.exerciseCount,
                    variantCount = libraryCounts.variantCount,
                    machineCount = libraryCounts.machineCount,
                    isAdoptingTemplate = isAdoptingTemplate,
                    onCreateProgram = { showCreateProgramDialog = true },
                    onUseTemplate = { viewModel.adoptNickWalkerTemplate() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            item(key = "next_card") {
                val totalSets = activeUiState.exercises.sumOf { it.sessionSets }
                val doneSets = activeUiState.exercises.sumOf { detail -> detail.sets.count { it.setType == SetType.WORK } }
                NextWorkoutCard(
                    todayLabel = todayLabel(nowMillis),
                    title = activeSession?.title ?: recommendedProgramDay?.title ?: systemStatus.todayWorkoutTitle,
                    summary = if (activeSession == null) recommendedProgramDay?.let { daySummaries[it.id] } else null,
                    isRecovered = systemStatus.fatigueStatus == "Normal" || systemStatus.fatigueStatus == "Fully recovered",
                    fatigueLabel = systemStatus.fatigueStatus,
                    lastTitle = systemStatus.lastWorkoutTitle,
                    lastDaysAgo = systemStatus.lastWorkoutDaysAgo,
                    recoveryText = systemStatus.recoveryDaysText,
                    readinessScore = systemStatus.lastReadinessScore,
                    activeSession = activeSession?.let {
                        ActiveSessionProgress(it.title, { if (it.isRetroactive) it.durationMinutes * 60L else clock.value.elapsedSeconds }, doneSets, totalSets)
                    },
                    isStarting = isStarting,
                    startError = startError,
                    onStart = recommendedProgramDay?.let { day -> { startDay(day) } },
                    onResume = onNavigateToActiveWorkout,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "week_header") {
                WeekHeader(
                    sessions = programDays.count { !it.isRestDay },
                    restDays = programDays.count { it.isRestDay },
                    rotation = rotation,
                    onScheduleModeChange = activeProgram?.let { program ->
                        { useRotation -> viewModel.setScheduleMode(program.id, if (useRotation) SCHEDULE_ROTATION else SCHEDULE_WEEKDAYS) }
                    },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
                )
            }
            items(weekRows, key = { it.day.id }) { row ->
                WeekDayItem(
                    row = row,
                    summary = daySummaries[row.day.id],
                    onOpen = { openDay(row.day) },
                    onAddExercise = {
                        targetProgramDayId = row.day.id
                        showAddExerciseDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            if (activeSession == null) item(key = "log_past") {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { showPastWorkout = true }, modifier = Modifier.testTag("log_past_workout")) {
                        Text("Log a past workout", style = MutantType.ButtonSmall, color = MutantColors.Primary)
                    }
                }
            }
        }
    }

    if (showPastWorkout) {
        PastWorkoutSheet(
            trainingDays = programDays.filterNot { it.isRestDay }.sortedBy { it.dayIndex },
            defaultDayId = recommendedProgramDay?.id,
            isStarting = isStarting,
            onConfirm = { day, startedAt, minutes ->
                viewModel.startPastWorkout(day, startedAt, minutes) {
                    showPastWorkout = false
                    onNavigateToActiveWorkout()
                }
            },
            onDismiss = { showPastWorkout = false }
        )
    }

    if (showAddExerciseDialog) {
        val day = programDays.firstOrNull { it.id == targetProgramDayId } ?: recommendedProgramDay
        if (day != null) {
            AddProgramExerciseSheet(
                dayTitle = day.title,
                exercises = allExercises,
                variants = allVariants,
                onAdd = { exercise, variantId ->
                    viewModel.addExerciseToProgram(
                        dayId = day.id,
                        exerciseId = exercise.id,
                        variantId = variantId,
                        sets = exercise.defaultWorkSets,
                        repMin = exercise.defaultRepMin,
                        repMax = exercise.defaultRepMax,
                        rir = exercise.defaultRir,
                        rest = exercise.defaultRestSeconds
                    )
                    showAddExerciseDialog = false
                    toast.show("${exercise.baseName.ifBlank { exercise.name }} added to ${day.title}")
                },
                onCreateVariant = { exercise ->
                    selectedExerciseForVariant = exercise
                    showCreateVariantDialog = true
                },
                onDismiss = { showAddExerciseDialog = false }
            )
        }
    }

    val variantFor = selectedExerciseForVariant
    if (showCreateVariantDialog && variantFor != null) {
        CustomVariantSheet(
            exercise = variantFor,
            onSave = { input ->
                viewModel.createCustomVariant(
                    exerciseId = variantFor.id,
                    exerciseStableId = variantFor.stableId,
                    variantName = input.name,
                    manufacturer = input.manufacturer,
                    resistanceType = input.resistanceType,
                    seat = input.seat,
                    handle = input.handle
                )
                showCreateVariantDialog = false
                toast.show("Variant saved")
            },
            onDismiss = { showCreateVariantDialog = false }
        )
    }

    if (showCreateProgramDialog) {
        CreateProgramSheet(
            onCreate = { name, description ->
                val days = listOf(
                    ProgramDay(dayIndex = 0, dayCode = "MON", title = "Push (Chest / Delts)", isRestDay = false, description = "Upper push"),
                    ProgramDay(dayIndex = 1, dayCode = "TUE", title = "Pull (Back / Biceps)", isRestDay = false, description = "Lat width and back density"),
                    ProgramDay(dayIndex = 2, dayCode = "WED", title = "Rest", isRestDay = true, description = "Recovery"),
                    ProgramDay(dayIndex = 3, dayCode = "THU", title = "Legs (Quad focus)", isRestDay = false, description = "Quads and hamstrings"),
                    ProgramDay(dayIndex = 4, dayCode = "FRI", title = "Upper (Chest / Back)", isRestDay = false, description = "Upper body density"),
                    ProgramDay(dayIndex = 5, dayCode = "SAT", title = "Arms + Delts", isRestDay = false, description = "Arm volume"),
                    ProgramDay(dayIndex = 6, dayCode = "SUN", title = "Rest", isRestDay = true, description = "Recovery")
                )
                viewModel.createCustomProgram(name, description, days)
                showCreateProgramDialog = false
                toast.show("$name created")
            },
            onDismiss = { showCreateProgramDialog = false }
        )
    }
}
