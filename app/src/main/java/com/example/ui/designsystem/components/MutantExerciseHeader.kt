package com.example.ui.designsystem.components

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantPalette
import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantSpacing

@Composable
fun MutantExerciseHeader(
    workoutTitle: String,
    exerciseIndex: Int,
    totalExercises: Int,
    exerciseName: String,
    manufacturer: String? = null,
    setupNotes: String? = null,
    onSwitchVariant: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
    ) {
        // Top breadcrumb row: WORKOUT TITLE + 1 / 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = workoutTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantPalette.TextMetadata,
                    letterSpacing = MutantTracking.Label
                )
            )

            Surface(
                shape = MutantShapeTokens.TinyControl,
                color = MutantPalette.SurfaceEmphasized
            ) {
                Text(
                    text = "$exerciseIndex / $totalExercises",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantPalette.PrimaryPurple
                    ),
                    modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.badgeInset)
                )
            }
        }

        // Exercise Title & Manufacturer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantPalette.TextPrimary,
                        letterSpacing = (-0.3).sp
                    )
                )

                if (!manufacturer.isNullOrEmpty()) {
                    Text(
                        text = manufacturer,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MutantPalette.SecondaryPurple
                        )
                    )
                }
            }

            if (onSwitchVariant != null) {
                Surface(
                    shape = MutantShapeTokens.TinyControl,
                    color = MutantPalette.SurfaceNormal,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Hairline, MutantPalette.SubtleBorder),
                    modifier = Modifier
                        .clickable { onSwitchVariant() }
                        .testTag("switch_variant_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.compact),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xxs)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch variant",
                            tint = MutantPalette.PrimaryPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Aparelho",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MutantPalette.PrimaryPurple
                            )
                        )
                    }
                }
            }
        }

        // Setup cues (Seat, handle, etc.)
        if (!setupNotes.isNullOrEmpty()) {
            Text(
                text = setupNotes,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MutantPalette.TextSecondary,
                    lineHeight = 16.sp
                )
            )
        }
    }
}
