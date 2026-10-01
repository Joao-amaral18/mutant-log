package com.example.ui.screens

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
import com.example.ui.components.SettingsDataDialog
import com.example.ui.components.GymPickerSheet
import com.example.ui.components.gymTag
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.components.LocalMutantToast
import com.example.ui.designsystem.components.MutantTopBar
import com.example.ui.designsystem.components.MutantPrimaryButton
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
    val weekRows = buildWeekRows(programDays, finishedWorkouts, recommendedProgramDay?.id, nowMillis)
    val openDay: (ProgramDay) -> Unit = { day ->
        val row = weekRows.firstOrNull { it.day.id == day.id }
        when {
            row?.status == WeekDayStatus.DONE -> onNavigateToHistory()
            activeSession != null -> onNavigateToActiveWorkout()
            else -> {
                selectedDayToStart = day
                showReadinessDialog = true
            }
        }
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
                val totalSets = activeUiState.exercises.sumOf { it.exercise.defaultWorkSets }
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
                    activeSession = activeSession?.let {
                        ActiveSessionProgress(it.title, activeUiState.elapsedSeconds, doneSets, totalSets)
                    },
                    isStarting = isStarting,
                    startError = startError,
                    onStart = recommendedProgramDay?.let { day -> { openDay(day) } },
                    onResume = onNavigateToActiveWorkout,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item(key = "week_header") {
                WeekHeader(
                    sessions = programDays.count { !it.isRestDay },
                    restDays = programDays.count { it.isRestDay },
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
        }
    }

    // --- DIALOG: ADD EXERCISE (Search [incline] -> Muscle -> Exercise -> Available variants -> [ADD] / [CREATE CUSTOM VARIANT]) ---
    if (showAddExerciseDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedMuscleFilter by remember { mutableStateOf("All") }
        var chosenExercise by remember { mutableStateOf<Exercise?>(null) }
        var chosenVariantId by remember { mutableStateOf("") }

        val filteredExercises = allExercises.filter { ex ->
            val matchesMuscle = selectedMuscleFilter == "All" || ex.muscleGroup.equals(selectedMuscleFilter, ignoreCase = true)
            val matchesQuery = searchQuery.isEmpty() ||
                    ex.name.contains(searchQuery, ignoreCase = true) ||
                    ex.baseName.contains(searchQuery, ignoreCase = true)
            matchesMuscle && matchesQuery
        }

        val availableVariantsForChosen = if (chosenExercise != null) {
            allVariants.filter { it.exerciseId == chosenExercise!!.id || it.exerciseStableId == chosenExercise!!.stableId }
        } else emptyList()

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = {
                Text(
                    text = "ADD EXERCISE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MutantVolt)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
                ) {
                    // Search Field: [incline]
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search (e.g. incline, press, squat)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MutantVolt) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = MutantTextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutantVolt,
                            unfocusedBorderColor = MutantBorder
                        )
                    )

                    // Muscle Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                    ) {
                        listOf("All", "Chest", "Back", "Quads", "Delts", "Biceps", "Triceps").forEach { m ->
                            val sel = selectedMuscleFilter == m
                            Surface(
                                modifier = Modifier.clickable { selectedMuscleFilter = m },
                                shape = MutantShapeTokens.SmallControl,
                                color = if (sel) MutantVolt else MutantDarkNavy,
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (sel) MutantVolt else MutantBorder)
                            ) {
                                Text(
                                    text = m,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (sel) MutantOnVolt else MutantTextSecondary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.xxs)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MutantBorder.copy(alpha = 0.5f))

                    if (chosenExercise == null) {
                        // Exercise Selection List
                        Text(
                            text = "SELECT EXERCISE (${filteredExercises.size} available)",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary, fontWeight = FontWeight.Bold)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                        ) {
                            items(filteredExercises) { ex ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            chosenExercise = ex
                                            val vars = allVariants.filter { it.exerciseId == ex.id || it.exerciseStableId == ex.stableId }
                                            chosenVariantId = vars.firstOrNull()?.id ?: ""
                                        },
                                    shape = MutantShapeTokens.TinyControl,
                                    color = MutantDarkNavy,
                                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                                ) {
                                    Column(modifier = Modifier.padding(MutantSpacing.compactMd)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = ex.baseName.ifEmpty { ex.name },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MutantTextPrimary
                                                )
                                            )
                                            Text(
                                                text = ex.muscleGroup,
                                                style = MaterialTheme.typography.labelSmall.copy(color = MutantVolt)
                                            )
                                        }
                                        Text(
                                            text = "${ex.defaultWorkSets} work sets · ${ex.defaultRepMin}-${ex.defaultRepMax} reps · ${ex.movementPattern}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Chosen Exercise & Available Variants Selection
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MutantShapeTokens.CompactControl,
                            color = MutantSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt)
                        ) {
                            Row(
                                modifier = Modifier.padding(MutantSpacing.compactMd),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = chosenExercise!!.muscleGroup.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantVolt, fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = chosenExercise!!.baseName.ifEmpty { chosenExercise!!.name },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = MutantTextPrimary)
                                    )
                                }
                                TextButton(onClick = { chosenExercise = null }) {
                                    Text("Change", color = MutantCyan)
                                }
                            }
                        }

                        Text(
                            text = "AVAILABLE VARIANTS",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary, fontWeight = FontWeight.Bold)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                        ) {
                            items(availableVariantsForChosen) { v ->
                                val isSelected = chosenVariantId == v.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { chosenVariantId = v.id },
                                    shape = MutantShapeTokens.TinyControl,
                                    color = if (isSelected) MutantVolt.copy(alpha = 0.2f) else MutantDarkNavy,
                                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSelected) MutantVolt else MutantBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(MutantSpacing.compactMd),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = v.variantName,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MutantVolt else MutantTextPrimary
                                                )
                                            )
                                            Text(
                                                text = "${v.manufacturer} · ${v.resistanceType} · +${v.weightIncrementKg}kg",
                                                style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MutantVolt, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Create Custom Variant Button
                        OutlinedButton(
                            onClick = {
                                selectedExerciseForVariant = chosenExercise
                                showCreateVariantDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MutantShapeTokens.TinyControl,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(MutantSpacing.compact))
                            Text("CREATE CUSTOM VARIANT", color = MutantTextPrimary, fontSize = MutantTypeScale.compact)
                        }
                    }
                }
            },
            confirmButton = {
                if (chosenExercise != null) {
                    Button(
                        onClick = {
                            val dayId = targetProgramDayId ?: recommendedProgramDay?.id ?: 1L
                            viewModel.addExerciseToProgram(
                                dayId = dayId,
                                exerciseId = chosenExercise!!.id,
                                variantId = chosenVariantId,
                                sets = chosenExercise!!.defaultWorkSets,
                                repMin = chosenExercise!!.defaultRepMin,
                                repMax = chosenExercise!!.defaultRepMax,
                                rir = chosenExercise!!.defaultRir,
                                rest = chosenExercise!!.defaultRestSeconds
                            )
                            showAddExerciseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                    ) {
                        Text("ADD TO PROGRAM", fontWeight = FontWeight.Black)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Cancel", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }

    // --- DIALOG: CREATE CUSTOM VARIANT ---
    if (showCreateVariantDialog && selectedExerciseForVariant != null) {
        var customVariantName by remember { mutableStateOf("${selectedExerciseForVariant!!.baseName} — Custom") }
        var customMfg by remember { mutableStateOf("Gym Machine") }
        var customResType by remember { mutableStateOf("Plate-Loaded") }
        var customSeat by remember { mutableStateOf("Seat 3") }
        var customHandle by remember { mutableStateOf("Neutral") }

        AlertDialog(
            onDismissRequest = { showCreateVariantDialog = false },
            title = {
                Text(
                    text = "CREATE CUSTOM VARIANT",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MutantVolt)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                    Text(
                        text = "For: ${selectedExerciseForVariant!!.name}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                    )
                    OutlinedTextField(
                        value = customVariantName,
                        onValueChange = { customVariantName = it },
                        label = { Text("Variant Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customMfg,
                        onValueChange = { customMfg = it },
                        label = { Text("Manufacturer / Gym Equipment") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customResType,
                        onValueChange = { customResType = it },
                        label = { Text("Resistance Type (Plate-Loaded, Cable, etc.)") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                        OutlinedTextField(
                            value = customSeat,
                            onValueChange = { customSeat = it },
                            label = { Text("Seat") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customHandle,
                            onValueChange = { customHandle = it },
                            label = { Text("Handle") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCustomVariant(
                            exerciseId = selectedExerciseForVariant!!.id,
                            exerciseStableId = selectedExerciseForVariant!!.stableId,
                            variantName = customVariantName,
                            manufacturer = customMfg,
                            resistanceType = customResType,
                            seat = customSeat,
                            handle = customHandle
                        )
                        showCreateVariantDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                ) {
                    Text("SAVE VARIANT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateVariantDialog = false }) {
                    Text("Cancel", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }

    // --- DIALOG: CREATE PROGRAM ---
    if (showCreateProgramDialog) {
        var programName by remember { mutableStateOf("Custom Protocol") }
        var programDescription by remember { mutableStateOf("High-intensity overload split") }

        AlertDialog(
            onDismissRequest = { showCreateProgramDialog = false },
            title = {
                Text(
                    text = "CREATE TRAINING PROGRAM",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MutantVolt)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)) {
                    Text(
                        text = "Build a tailored weekly split or customize existing days.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MutantTextSecondary)
                    )
                    OutlinedTextField(
                        value = programName,
                        onValueChange = { programName = it },
                        label = { Text("Program Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MutantVolt,
                            unfocusedBorderColor = MutantBorder
                        )
                    )
                    OutlinedTextField(
                        value = programDescription,
                        onValueChange = { programDescription = it },
                        label = { Text("Description") },
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
                        val days = listOf(
                            ProgramDay(dayIndex = 0, dayCode = "SEG", title = "Push (Chest / Delts)", isRestDay = false, description = "Upper push hypertrophy"),
                            ProgramDay(dayIndex = 1, dayCode = "TER", title = "Pull (Back / Biceps)", isRestDay = false, description = "Lat width and back density"),
                            ProgramDay(dayIndex = 2, dayCode = "QUA", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery"),
                            ProgramDay(dayIndex = 3, dayCode = "QUI", title = "Legs (Quad Dominant)", isRestDay = false, description = "Quad overload & hack squat"),
                            ProgramDay(dayIndex = 4, dayCode = "SEX", title = "Upper (Chest / Back)", isRestDay = false, description = "Upper body density"),
                            ProgramDay(dayIndex = 5, dayCode = "SÁB", title = "Arms + Delts", isRestDay = false, description = "Arm hypertrophy"),
                            ProgramDay(dayIndex = 6, dayCode = "DOM", title = "Rest", isRestDay = true, description = "Rest day · Systemic recovery")
                        )
                        viewModel.createCustomProgram(programName, programDescription, days)
                        showCreateProgramDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutantVolt, contentColor = MutantOnVolt)
                ) {
                    Text("CREATE & INITIALIZE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProgramDialog = false }) {
                    Text("Cancel", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }
}
