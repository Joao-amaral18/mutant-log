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
    modifier: Modifier = Modifier
) {
    val isStarting by viewModel.isStartingWorkout.collectAsState()
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

    val todayProgramDay = programDays.find { it.dayCode == "SEG" } ?: programDays.firstOrNull { !it.isRestDay }

    if (showSettingsDialog) {
        SettingsDataDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showReadinessDialog && selectedDayToStart != null) {
        ReadinessDialog(
            onDismiss = { showReadinessDialog = false },
            onConfirm = { readinessInput ->
                showReadinessDialog = false
                viewModel.startWorkout(selectedDayToStart!!, readinessInput, onNavigateToActiveWorkout)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MutantBlack)
            .padding(horizontal = MutantSpacing.md),
        contentPadding = PaddingValues(top = MutantSpacing.md, bottom = MutantSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
    ) {
        if (isStarting) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text("Starting workout...", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        startError?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        // App Header & OS Identifier
        item {
            MutantTopBar(
                gymName = selectedGym?.name,
                onGymClick = { showGymPicker = true },
                onSettingsClick = { showSettingsDialog = true }
            )
        }
        // --- FIRST-RUN STATE: No Active Program ---
        if (programDays.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_program_card"),
                    shape = MutantShapeTokens.LargePanel,
                    colors = CardDefaults.cardColors(containerColor = MutantSurface),
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(MutantSpacing.lgMd),
                        verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MUTANT LOG",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantVolt,
                                    letterSpacing = MutantTracking.Brand
                                )
                            )
                            Surface(
                                color = MutantDarkNavy,
                                shape = MutantShapeTokens.SmallControl,
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                            ) {
                                Text(
                                    text = "FRESH INSTALL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MutantCyan,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.badgeInset)
                                )
                            }
                        }

                        Text(
                            text = "No active program",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MutantTextPrimary
                            )
                        )

                        Text(
                            text = "The canonical reference catalog is preloaded with zero fabricated user history. Start fresh by creating your protocol or loading the Nick Walker reconstruction template.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MutantTextSecondary)
                        )

                        HorizontalDivider(color = MutantBorder.copy(alpha = 0.6f))

                        // Catalog Metrics Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MutantShapeTokens.InputChip,
                                color = MutantDarkNavy,
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                            ) {
                                Column(modifier = Modifier.padding(MutantSpacing.mdPlus)) {
                                    Text(
                                        text = "Exercise library",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                    Text(
                                        text = "${libraryCounts.exerciseCount} exercises",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MutantTextPrimary
                                        )
                                    )
                                    Text(
                                        text = "available",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = MutantShapeTokens.InputChip,
                                color = MutantDarkNavy,
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                            ) {
                                Column(modifier = Modifier.padding(MutantSpacing.mdPlus)) {
                                    Text(
                                        text = "Machine catalog",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                    )
                                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                    Text(
                                        text = "${libraryCounts.variantCount} variants",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MutantVolt
                                        )
                                    )
                                    Text(
                                        text = "${libraryCounts.machineCount} machines",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Button(
                            onClick = { showCreateProgramDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_program_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutantVolt,
                                contentColor = MutantOnVolt
                            ),
                            shape = MutantShapeTokens.CompactControl
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(MutantSpacing.xs))
                            Text(
                                text = "CREATE PROGRAM",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = MutantTracking.Compact
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.adoptNickWalkerTemplate() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("use_nick_walker_template_button"),
                            shape = MutantShapeTokens.CompactControl,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantVolt)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(MutantSpacing.xs))
                            Text("USE NICK WALKER TEMPLATE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Today's training focus
            item {
                val isRestDay = todayProgramDay?.isRestDay == true
                val statusColor = if (systemStatus.fatigueStatus == "Normal") MutantEmerald else MutantAmber
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_status_card"),
                    shape = MutantShapeTokens.LargePanel,
                    colors = CardDefaults.cardColors(containerColor = MutantBlack)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 248.dp)) {
                        Image(
                            painter = painterResource(R.drawable.mutant_workout_hero),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier.matchParentSize().background(
                                Brush.horizontalGradient(
                                    0f to MutantBlack.copy(alpha = 0.82f),
                                    0.48f to MutantBlack.copy(alpha = 0.58f),
                                    1f to MutantBlack.copy(alpha = 0.12f)
                                )
                            )
                        )
                        Box(
                            modifier = Modifier.matchParentSize().background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    1f to MutantBlack.copy(alpha = 0.40f)
                                )
                            )
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(MutantSpacing.lg),
                            verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                        ) {
                            Text(
                                text = "TODAY",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantTextSecondary,
                                    letterSpacing = MutantTracking.Section
                                )
                            )
                            Text(
                                text = systemStatus.todayWorkoutTitle,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MutantTextPrimary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(9.dp).clip(CircleShape).background(
                                        if (isRestDay) MutantTextMuted else statusColor
                                    )
                                )
                                Spacer(modifier = Modifier.width(MutantSpacing.xs))
                                Text(
                                    text = when {
                                        isRestDay -> "Rest day"
                                        systemStatus.fatigueStatus == "Normal" -> "Ready"
                                        else -> systemStatus.fatigueStatus
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRestDay) MutantTextSecondary else statusColor
                                    )
                                )
                            }
                            Text(
                                text = "Next: ${systemStatus.nextScheduledTitle}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MutantTextSecondary)
                            )
                            if (!isRestDay && activeSession == null && todayProgramDay != null) {
                                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                                MutantPrimaryButton(
                                    text = "START WORKOUT",
                                    onClick = {
                                        selectedDayToStart = todayProgramDay
                                        showReadinessDialog = true
                                    },
                                    modifier = Modifier.width(260.dp).testTag("start_workout_button"),
                                    height = 52.dp,
                                    icon = Icons.Default.PlayArrow,
                                    testTag = "start_workout_button"
                                )
                            }
                        }
                    }
                }
            }
        // Active Session Takeover Banner if in progress
        if (activeSession != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToActiveWorkout() }
                        .testTag("resume_workout_banner"),
                    shape = MutantShapeTokens.Panel,
                    color = MutantSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Emphasized, MutantVolt)
                ) {
                    Column(
                        modifier = Modifier.padding(MutantSpacing.lgCompact),
                        verticalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MutantVolt)
                                )
                                Spacer(modifier = Modifier.width(MutantSpacing.xs))
                                Text(
                                    text = "SESSION IN PROGRESS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MutantVolt,
                                        letterSpacing = MutantTracking.Label
                                    )
                                )
                            }

                            val completedCount = activeUiState.exercises.count { exDetail ->
                                exDetail.sets.any { s -> s.setType == SetType.WORK }
                            }
                            val totalCount = activeUiState.exercises.size
                            Text(
                                text = if (totalCount > 0) "$completedCount / $totalCount exercises" else "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MutantTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Text(
                            text = activeSession?.title ?: "Workout",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantTextPrimary
                            )
                        )

                        Button(
                            onClick = onNavigateToActiveWorkout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("resume_workout_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutantVolt,
                                contentColor = MutantOnVolt
                            ),
                            shape = MutantShapeTokens.CompactControl
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MutantOnVolt)
                            Spacer(modifier = Modifier.width(MutantSpacing.xs))
                            Text(
                                text = "RESUME SESSION",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = MutantTracking.Compact
                                )
                            )
                        }
                    }
                }
            }
        }

            // Program Structure Header & Add Exercise
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THIS WEEK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantTextSecondary,
                            letterSpacing = MutantTracking.Section
                        )
                    )
                    TextButton(
                        onClick = {
                            targetProgramDayId = todayProgramDay?.id ?: programDays.firstOrNull()?.id
                            showAddExerciseDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MutantVolt)
                        Spacer(modifier = Modifier.width(MutantSpacing.xxs))
                        Text("ADD EXERCISE", color = MutantVolt, fontWeight = FontWeight.Bold, fontSize = MutantTypeScale.compact)
                    }
                }
            }

            // Weekly Days List
            items(programDays) { day ->
                val isToday = day.id == todayProgramDay?.id
                val rowMarker = when {
                    isToday -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    day.isRestDay -> Modifier.background(MutantTextMuted.copy(alpha = 0.58f), CircleShape)
                    else -> Modifier.border(2.dp, MutantBorder, CircleShape)
                }
                Column {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !day.isRestDay && activeSession == null) {
                                selectedDayToStart = day
                                showReadinessDialog = true
                            }
                            .testTag("program_day_${day.dayIndex}"),
                        shape = MutantShapeTokens.InputChip,
                        color = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = MutantSpacing.sm, vertical = MutantSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                        ) {
                            Text(
                                text = day.dayCode,
                                modifier = Modifier.width(34.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isToday) MutantVolt else MutantTextMuted
                                )
                            )
                            Box(modifier = Modifier.size(12.dp).then(rowMarker))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = day.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (day.isRestDay) MutantTextMuted else MutantTextPrimary
                                    )
                                )
                                if (isToday || day.isRestDay) {
                                    Text(
                                        text = if (isToday) "Today" else "Rest",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                    )
                                }
                            }
                            if (!day.isRestDay && activeSession == null) {
                                IconButton(
                                    onClick = {
                                        targetProgramDayId = day.id
                                        showAddExerciseDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add exercise to ${day.title}", tint = MutantTextMuted)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MutantTextMuted)
                        }
                    }
                    if (day != programDays.lastOrNull()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = MutantSpacing.weekDividerIndent),
                            color = MutantBorder.copy(alpha = 0.38f)
                        )
                    }
                }
            }

            item {
                MutantCard(
                    variant = MutantCardVariant.CONTAINER_LOW,
                    contentPadding = MutantSpacing.md
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MutantVolt,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Last workout", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                            Text(systemStatus.lastSessionTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary))
                        }
                        VerticalDivider(modifier = Modifier.height(48.dp), color = MutantBorder)
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MutantVolt,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recovery", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                            Text(systemStatus.recoveryDaysText, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MutantEmerald))
                            Text(systemStatus.fatigueStatus, style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted))
                        }
                    }
                }
            }        }
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
                            val dayId = targetProgramDayId ?: todayProgramDay?.id ?: 1L
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

    // Gym Picker Dialog
    if (showGymPicker) {
        AlertDialog(
            onDismissRequest = { showGymPicker = false },
            title = {
                Text(
                    text = "SELECT GYM FACILITY",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MutantVolt)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                    allGyms.forEach { gym ->
                        val isSelected = selectedGym?.id == gym.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectGym(gym)
                                    showGymPicker = false
                                },
                            shape = MutantShapeTokens.TinyControl,
                            color = if (isSelected) MutantVolt.copy(alpha = 0.2f) else MutantDarkNavy,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MutantVolt else MutantBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(MutantSpacing.sm)) {
                                Text(
                                    text = gym.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MutantVolt else MutantTextPrimary
                                    )
                                )
                                if (gym.notes.isNotEmpty()) {
                                    Text(
                                        text = gym.notes,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGymPicker = false }) {
                    Text("Close", color = MutantTextSecondary)
                }
            },
            containerColor = MutantSurfaceCard
        )
    }
}
