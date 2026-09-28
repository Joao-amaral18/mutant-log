package com.example.ui.screens

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Exercise
import com.example.data.model.MachineCatalogEntity
import com.example.data.model.ProgressionRecommendation
import com.example.data.model.WorkoutSet
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import com.example.ui.designsystem.components.MutantScreenHeader
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExerciseHistoryScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allExercises by viewModel.allExercises.collectAsState(initial = emptyList())
    val allMachines by viewModel.allMachineCatalog.collectAsState(initial = emptyList())
    val allVariants by viewModel.allVariants.collectAsState(initial = emptyList())
    val libraryCounts by viewModel.libraryCounts.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0 = Exercise Library, 1 = Machine Catalog
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf("All") }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var selectedManufacturerFilter by remember { mutableStateOf("All") }

    LaunchedEffect(allExercises) {
        if (selectedExercise == null && allExercises.isNotEmpty()) {
            selectedExercise = allExercises.first()
        }
    }

    var historySets by remember { mutableStateOf<List<WorkoutSet>>(emptyList()) }
    var progressionRecommendation by remember { mutableStateOf<ProgressionRecommendation?>(null) }

    LaunchedEffect(selectedExercise) {
        val ex = selectedExercise ?: return@LaunchedEffect
        viewModel.repository.getExerciseHistory(ex.id).collect { list ->
            historySets = list
        }
    }

    LaunchedEffect(selectedExercise) {
        val ex = selectedExercise ?: return@LaunchedEffect
        val rec = viewModel.repository.getProgressionSuggestion(
            exerciseId = ex.id,
            repMin = ex.defaultRepMin,
            repMax = ex.defaultRepMax,
            targetRir = ex.defaultRir,
            incrementKg = ex.defaultIncrementKg
        )
        progressionRecommendation = rec
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MutantBlack)
            .padding(horizontal = MutantSpacing.md),
        contentPadding = PaddingValues(top = MutantSpacing.md, bottom = MutantSpacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.mdPlus)
    ) {
        item {
            MutantScreenHeader(
                eyebrow = "CANONICAL DOSSIER",
                title = if (activeTab == 0) "Exercise Library (${libraryCounts.exerciseCount})" else "Machine Catalog (${libraryCounts.machineCount})",
                onBack = onNavigateBack
            )
        }

        // Segmented Control: Exercise Library vs Machine Catalog
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MutantShapeTokens.CompactControl,
                color = MutantDarkNavy,
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Row(modifier = Modifier.padding(MutantSpacing.xxs)) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = 0 },
                        shape = MutantShapeTokens.TinyControl,
                        color = if (activeTab == 0) MutantVolt else androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = MutantSpacing.xs)) {
                            Text(
                                text = "EXERCISE LIBRARY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (activeTab == 0) MutantOnVolt else MutantTextSecondary
                                )
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = 1 },
                        shape = MutantShapeTokens.TinyControl,
                        color = if (activeTab == 1) MutantVolt else androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = MutantSpacing.xs)) {
                            Text(
                                text = "MACHINE CATALOG",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = if (activeTab == 1) MutantOnVolt else MutantTextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Search Input
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search catalog (e.g. panatta, incline, hack)") },
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
        }

        if (activeTab == 0) {
            // --- EXERCISE LIBRARY TAB ---
            // Muscle Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                ) {
                    listOf("All", "Chest", "Back", "Quads", "Hamstrings", "Delts", "Biceps", "Triceps").forEach { m ->
                        val isSel = selectedMuscleFilter == m
                        Surface(
                            modifier = Modifier.clickable { selectedMuscleFilter = m },
                            shape = MutantShapeTokens.SmallControl,
                            color = if (isSel) MutantVolt else MutantSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSel) MutantVolt else MutantBorder)
                        ) {
                            Text(
                                text = m,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MutantOnVolt else MutantTextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.xxs)
                            )
                        }
                    }
                }
            }

            // Exercise Horizontal Quick Switcher
            val matchingExercises = allExercises.filter { ex ->
                val mMatch = selectedMuscleFilter == "All" || ex.muscleGroup.equals(selectedMuscleFilter, ignoreCase = true)
                val qMatch = searchQuery.isEmpty() || ex.name.contains(searchQuery, ignoreCase = true) || ex.baseName.contains(searchQuery, ignoreCase = true)
                mMatch && qMatch
            }

            item {
                Text(
                    text = "SELECT EXERCISE (${matchingExercises.size} found)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(MutantSpacing.compact))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MutantSpacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                ) {
                    matchingExercises.take(4).forEach { ex ->
                        val isSelected = selectedExercise?.id == ex.id
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedExercise = ex },
                            shape = MutantShapeTokens.TinyControl,
                            color = if (isSelected) MutantVolt else MutantSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MutantVolt else MutantBorder
                            )
                        ) {
                            Text(
                                text = ex.baseName.ifEmpty { ex.name.take(14) },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                    color = if (isSelected) MutantOnVolt else MutantTextSecondary,
                                    fontSize = MutantTypeScale.micro
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.compact, vertical = MutantSpacing.xs),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            val ex = selectedExercise
            if (ex != null) {
                // Bio-mechanical Specification Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MutantShapeTokens.Panel,
                        colors = CardDefaults.cardColors(containerColor = MutantSurface),
                        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                    ) {
                        Column(modifier = Modifier.padding(MutantSpacing.md)) {
                            Text(
                                text = ex.name,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MutantTextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                            Text(
                                text = "Pattern: ${ex.movementPattern} • Group: ${ex.muscleGroup}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MutantVolt)
                            )

                            Spacer(modifier = Modifier.height(MutantSpacing.sm))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Seat Setup", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                                    Text(ex.seatPosition.ifEmpty { "Standard" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Handle/Lever", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                                    Text(ex.handlePosition.ifEmpty { "Neutral" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Increment", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                                    Text("+${ex.defaultIncrementKg} kg", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantVolt))
                                }
                            }

                            if (ex.executionCues.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(MutantSpacing.sm))
                                HorizontalDivider(color = MutantBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                                Text("Execution Cues", style = MaterialTheme.typography.labelSmall.copy(color = MutantVolt, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Text(
                                    text = ex.executionCues,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary, lineHeight = 18.sp)
                                )
                            }

                            // Available Variants list for this exercise
                            val exVariants = allVariants.filter { it.exerciseId == ex.id || it.exerciseStableId == ex.stableId }
                            if (exVariants.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(MutantSpacing.sm))
                                HorizontalDivider(color = MutantBorder.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                                Text("Available Variants (${exVariants.size})", style = MaterialTheme.typography.labelSmall.copy(color = MutantCyan, fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(MutantSpacing.compact))
                                exVariants.forEach { v ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = MutantSpacing.micro),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "• ${v.variantName}", style = MaterialTheme.typography.bodySmall.copy(color = MutantTextPrimary, fontWeight = FontWeight.SemiBold))
                                        Text(text = "${v.resistanceType} (+${v.weightIncrementKg}kg)", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted))
                                    }
                                }
                            }
                        }
                    }
                }

                // Progression Suggestion Card
                item {
                    val rec = progressionRecommendation
                    if (rec != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MutantShapeTokens.Panel,
                            colors = CardDefaults.cardColors(containerColor = MutantSurfaceElevated),
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(MutantSpacing.md)) {
                                Text(
                                    text = "TARGET & SUGGESTION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantVolt,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Text(
                                    text = "${rec.suggestedWeightKg} kg × ${rec.suggestedRepsMin}–${rec.suggestedRepsMax} @ ${rec.targetRir} RIR",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MutantTextPrimary)
                                )
                                Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                                Text(
                                    text = rec.reason,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                                )
                            }
                        }
                    }
                }

                // Workout Performance History
                item {
                    Text(
                        text = "WORKOUT HISTORY (${historySets.size} sets logged)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantTextSecondary,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                }

                if (historySets.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MutantShapeTokens.CompactControl,
                            color = MutantDarkNavy,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                        ) {
                            Box(modifier = Modifier.padding(MutantSpacing.lgMd), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No workouts logged yet. Complete a session to track progressive overload.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MutantTextMuted)
                                )
                            }
                        }
                    }
                } else {
                    items(historySets) { s ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MutantShapeTokens.TinyControl,
                            color = MutantDarkNavy,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = MutantSpacing.sm, vertical = MutantSpacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${dateFormat.format(Date(s.completedAt))} · Set #${s.setNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                                )
                                Text(
                                    text = "${s.weightKg} kg × ${s.reps} @ ${s.rir} RIR",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MutantTextPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- MACHINE CATALOG TAB (186 Models) ---
            item {
                // Manufacturer Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                ) {
                    listOf("All", "Panatta", "Hammer Strength", "Prime", "Arsenal Strength", "Cybex", "Technogym").forEach { mfg ->
                        val isSel = selectedManufacturerFilter == mfg
                        Surface(
                            modifier = Modifier.clickable { selectedManufacturerFilter = mfg },
                            shape = MutantShapeTokens.SmallControl,
                            color = if (isSel) MutantVolt else MutantSurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSel) MutantVolt else MutantBorder)
                        ) {
                            Text(
                                text = mfg,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MutantOnVolt else MutantTextSecondary
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.xxs)
                            )
                        }
                    }
                }
            }

            val matchingMachines = allMachines.filter { m ->
                val mfgMatch = selectedManufacturerFilter == "All" || m.manufacturer.equals(selectedManufacturerFilter, ignoreCase = true)
                val qMatch = searchQuery.isEmpty() || m.model.contains(searchQuery, ignoreCase = true) || (m.manufacturer?.contains(searchQuery, ignoreCase = true) == true)
                mfgMatch && qMatch
            }

            item {
                Text(
                    text = "CANONICAL MACHINES (${matchingMachines.size} models)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantTextSecondary
                    )
                )
            }

            items(matchingMachines) { machine ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MutantShapeTokens.CompactControl,
                    color = MutantDarkNavy,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                ) {
                    Column(modifier = Modifier.padding(MutantSpacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = machine.model,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantTextPrimary
                                )
                            )
                            Surface(
                                shape = MutantShapeTokens.Compact,
                                color = MutantSurfaceElevated
                            ) {
                                Text(
                                    text = machine.equipmentType,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MutantVolt,
                                        fontSize = MutantTypeScale.micro
                                    ),
                                    modifier = Modifier.padding(horizontal = MutantSpacing.compact, vertical = MutantSpacing.micro)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Manufacturer: ${machine.manufacturer ?: "Generic"}",
                                style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                            )
                            Text(
                                text = "Increment: +${machine.defaultIncrementKg}kg",
                                style = MaterialTheme.typography.labelSmall.copy(color = MutantCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}
