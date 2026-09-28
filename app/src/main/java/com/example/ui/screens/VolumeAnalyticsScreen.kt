package com.example.ui.screens

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantTypeScale

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import com.example.ui.designsystem.components.MutantScreenHeader

data class MuscleVolumeData(
    val muscle: String,
    val directSets: Int,
    val indirectSets: Int,
    val targetBracket: String = "8–14 sets"
)

@Composable
fun VolumeAnalyticsScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finishedWorkouts by viewModel.finishedWorkouts.collectAsState(initial = emptyList())
    val systemStatus by viewModel.systemStatus.collectAsState()

    // 7-day hard set data
    val volumeList = remember {
        listOf(
            MuscleVolumeData("Chest", 8, 2, "8–12 sets"),
            MuscleVolumeData("Back", 10, 4, "10–14 sets"),
            MuscleVolumeData("Quads", 7, 0, "6–10 sets"),
            MuscleVolumeData("Hamstrings", 6, 2, "6–10 sets"),
            MuscleVolumeData("Delts", 9, 4, "8–12 sets"),
            MuscleVolumeData("Biceps", 6, 4, "6–10 sets"),
            MuscleVolumeData("Triceps", 6, 5, "6–10 sets")
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MutantBlack)
            .padding(horizontal = MutantSpacing.md),
        contentPadding = PaddingValues(top = MutantSpacing.md, bottom = MutantSpacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
    ) {
        item {
            MutantScreenHeader("SYSTEM METRICS & VOLUME", "Fadiga & Hard Sets", onNavigateBack)
        }

        // Fatigue Accumulation Diagnostic Card (Section 14)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fatigue_diagnostic_card"),
                shape = MutantShapeTokens.Panel,
                colors = CardDefaults.cardColors(containerColor = MutantSurface),
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Column(modifier = Modifier.padding(MutantSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FATIGUE ACCUMULATION DETECTOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantVolt,
                                letterSpacing = MutantTracking.Compact
                            )
                        )
                        Surface(
                            shape = MutantShapeTokens.SmallControl,
                            color = MutantEmerald.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantEmerald)
                        ) {
                            Text(
                                text = "STABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MutantEmerald
                                ),
                                modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.badgeInset)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.compactMd))
                    Text(
                        text = "O sistema monitora a tendência das últimas sessões (performance, sono, prontidão neuromuscular e desconforto articular).",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(MutantSpacing.sm))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                        StatusMiniBox(title = "Last 3 Sessions", value = "↑ Overload OK", color = MutantEmerald, modifier = Modifier.weight(1f))
                        StatusMiniBox(title = "Joint Health", value = "Optimal", color = MutantCyan, modifier = Modifier.weight(1f))
                        StatusMiniBox(title = "Systemic Fatigue", value = "Low / Normal", color = MutantVolt, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))
                    Text(
                        text = "Operational Protocol Recommendations:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary)
                    )
                    Spacer(modifier = Modifier.height(MutantSpacing.compact))
                    Row(horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("Maintain Protocol") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MutantVolt.copy(alpha = 0.2f),
                                selectedLabelColor = MutantVolt
                            )
                        )
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("Reduce Work Sets (-1)") },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MutantTextSecondary
                            )
                        )
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text("+1 Day Recovery") },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = MutantTextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Hard Sets Volume (Last 7 Days) — Section 18 of user prompt
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_volume_panel"),
                shape = MutantShapeTokens.Panel,
                colors = CardDefaults.cardColors(containerColor = MutantSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Column(modifier = Modifier.padding(MutantSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VOLUME EM HARD SETS (ÚLTIMOS 7 DIAS)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantVolt,
                                letterSpacing = MutantTracking.Compact
                            )
                        )
                        Text(
                            text = "Direct + Indirect",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                        )
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.compact))
                    Text(
                        text = "Apenas séries efetivas (work sets) levadas a 0–1 RIR são contabilizadas.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(MutantSpacing.md))

                    volumeList.forEach { item ->
                        VolumeProgressBarRow(item)
                        Spacer(modifier = Modifier.height(MutantSpacing.compactMd))
                    }
                }
            }
        }

        // PR Types Audit (Section 23 of prompt)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MutantShapeTokens.Panel,
                colors = CardDefaults.cardColors(containerColor = MutantSurface),
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Column(modifier = Modifier.padding(MutantSpacing.md)) {
                    Text(
                        text = "RECORDES RECENTES (PRS AUDITADOS)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MutantVolt,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                    Text(
                        text = "Diferenciação estrita entre Carga, Repetições e Técnica:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary)
                    )

                    Spacer(modifier = Modifier.height(MutantSpacing.sm))

                    PrRecordItem(
                        exercise = "Linear Hack Squat",
                        oldRecord = "180 kg × 15",
                        newRecord = "180 kg × 16",
                        type = "REP PR",
                        typeColor = MutantCyan
                    )

                    Spacer(modifier = Modifier.height(MutantSpacing.xs))

                    PrRecordItem(
                        exercise = "Horizontal Chest Press",
                        oldRecord = "100 kg × 10 (compromised form)",
                        newRecord = "95 kg × 12 (strict execution)",
                        type = "TECHNIQUE PR",
                        typeColor = MutantEmerald
                    )

                    Spacer(modifier = Modifier.height(MutantSpacing.xs))

                    PrRecordItem(
                        exercise = "Incline Machine Press",
                        oldRecord = "87.5 kg × 12",
                        newRecord = "90 kg × 11",
                        type = "LOAD PR",
                        typeColor = MutantAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusMiniBox(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MutantShapeTokens.TinyControl,
        color = MutantDarkNavy,
        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
    ) {
        Column(modifier = Modifier.padding(MutantSpacing.xs)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted, fontSize = MutantTypeScale.micro))
            Spacer(modifier = Modifier.height(MutantSpacing.micro))
            Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = color))
        }
    }
}

@Composable
private fun VolumeProgressBarRow(item: MuscleVolumeData) {
    val totalSets = item.directSets + item.indirectSets
    val maxProgress = 16f
    val directRatio = (item.directSets / maxProgress).coerceIn(0f, 1f)
    val indirectRatio = (item.indirectSets / maxProgress).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.muscle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary)
            )
            Text(
                text = "${item.directSets} direct • ${item.indirectSets} indirect (${totalSets} total)",
                style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
            )
        }

        Spacer(modifier = Modifier.height(MutantSpacing.xxs))

        // Multi-segment progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MutantShapeTokens.Compact)
                .background(MutantDarkNavy)
        ) {
            if (directRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(directRatio.coerceAtLeast(0.01f))
                        .background(MutantVolt)
                )
            }
            if (indirectRatio > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(indirectRatio.coerceAtLeast(0.01f))
                        .background(MutantCyan)
                )
            }
            val remaining = (1f - directRatio - indirectRatio).coerceAtLeast(0f)
            if (remaining > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(remaining)
                        .background(Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun PrRecordItem(
    exercise: String,
    oldRecord: String,
    newRecord: String,
    type: String,
    typeColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MutantDarkNavy,
        shape = MutantShapeTokens.CompactControl,
        border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MutantSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(exercise, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MutantTextPrimary))
                Text("$oldRecord → $newRecord", style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary))
            }
            Surface(
                color = typeColor.copy(alpha = 0.2f),
                shape = MutantShapeTokens.SmallControl,
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, typeColor)
            ) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = typeColor),
                    modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.badgeInset)
                )
            }
        }
    }
}
