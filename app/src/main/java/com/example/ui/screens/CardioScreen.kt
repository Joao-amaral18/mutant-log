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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.components.MutantPrimaryButton
import com.example.ui.designsystem.components.MutantScreenHeader
import com.example.data.model.CardioSession
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CardioScreen(
    viewModel: MutantViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardioSessions by viewModel.cardioSessions.collectAsState(initial = emptyList())

    var selectedMachine by remember { mutableStateOf("Stairmaster") }
    var durationInput by remember { mutableStateOf("20") }
    var levelInput by remember { mutableStateOf("6") }
    var hrInput by remember { mutableStateOf("142") }
    var rpeInput by remember { mutableStateOf("5") }
    var notesInput by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MutantBlack)
            .padding(horizontal = MutantSpacing.md),
        contentPadding = PaddingValues(top = MutantSpacing.md, bottom = MutantSpacing.screenBottom),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
    ) {
        item {
            MutantScreenHeader("CARDIOVASCULAR FLUSH", "Cardio Protocol", onNavigateBack)
        }

        // Cardio Logger Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cardio_logger_card"),
                shape = MutantShapeTokens.Panel,
                colors = CardDefaults.cardColors(containerColor = MutantSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Column(modifier = Modifier.padding(MutantSpacing.md)) {
                    Text(
                        text = "LOG CARDIO FLUSH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MutantVolt,
                            letterSpacing = MutantTracking.Compact
                        )
                    )
                    Text(
                        text = "Cardio separado para preservação de recuperação, sem confundir gasto calórico com progressão de hipertrofia.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MutantTextSecondary),
                        modifier = Modifier.padding(top = MutantSpacing.micro, bottom = MutantSpacing.sm)
                    )

                    // Machine Selector
                    Text("Machine / Equipment", style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary))
                    Spacer(modifier = Modifier.height(MutantSpacing.xxs))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                    ) {
                        listOf("Stairmaster", "Incline Walk", "Treadmill", "Bike").forEach { m ->
                            val isSelected = selectedMachine == m
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedMachine = m },
                                shape = MutantShapeTokens.SmallControl,
                                color = if (isSelected) MutantVolt else MutantDarkNavy,
                                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, if (isSelected) MutantVolt else MutantBorder)
                            ) {
                                Text(
                                    text = m,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                                        color = if (isSelected) MutantOnVolt else MutantTextSecondary,
                                        fontSize = MutantTypeScale.micro
                                    ),
                                    modifier = Modifier.padding(vertical = MutantSpacing.xs),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.sm))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)) {
                        OutlinedTextField(
                            value = durationInput,
                            onValueChange = { durationInput = it },
                            label = { Text("Duration (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MutantVolt, unfocusedBorderColor = MutantBorder)
                        )
                        OutlinedTextField(
                            value = levelInput,
                            onValueChange = { levelInput = it },
                            label = { Text("Level / Incline") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MutantVolt, unfocusedBorderColor = MutantBorder)
                        )
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.compactMd))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compactMd)) {
                        OutlinedTextField(
                            value = hrInput,
                            onValueChange = { hrInput = it },
                            label = { Text("HR Avg (bpm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MutantVolt, unfocusedBorderColor = MutantBorder)
                        )
                        OutlinedTextField(
                            value = rpeInput,
                            onValueChange = { rpeInput = it },
                            label = { Text("RPE (1-10)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MutantVolt, unfocusedBorderColor = MutantBorder)
                        )
                    }

                    Spacer(modifier = Modifier.height(MutantSpacing.mdPlus))

                    MutantPrimaryButton(
                        text = "SAVE CARDIO LOG",
                        onClick = {
                            val dur = durationInput.toIntOrNull() ?: 20
                            val lvl = levelInput.toIntOrNull() ?: 6
                            val hr = hrInput.toIntOrNull() ?: 140
                            val rpe = rpeInput.toIntOrNull() ?: 5
                            viewModel.logCardio(selectedMachine, dur, lvl, hr, rpe, notesInput)
                            showSuccessMessage = true
                        },
                        height = 48.dp,
                        testTag = "save_cardio_button"
                    )
                }
            }
        }

        // Historical Sessions List
        item {
            Text(
                text = "CARDIO LOGS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantTextSecondary,
                    letterSpacing = MutantTracking.Compact
                )
            )
        }

        items(cardioSessions) { session ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MutantShapeTokens.CompactControl,
                color = MutantSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MutantSpacing.mdPlus),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = session.machine.uppercase(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MutantVolt)
                        )
                        Spacer(modifier = Modifier.height(MutantSpacing.micro))
                        Text(
                            text = "${session.durationMinutes} min • Level ${session.level} • HR ${session.avgHeartRate} bpm • RPE ${session.rpe}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MutantTextPrimary)
                        )
                    }
                    Text(
                        text = dateFormat.format(Date(session.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(color = MutantTextMuted)
                    )
                }
            }
        }
    }
}
