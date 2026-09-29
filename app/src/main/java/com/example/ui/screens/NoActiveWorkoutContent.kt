package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantSpacing

@Composable
fun NoActiveWorkoutContent(
    lastSessionTitle: String?,
    lastSessionDaysAgo: Int?,
    hasEmptySession: Boolean = false,
    isDiscarding: Boolean = false,
    onNavigateToProtocols: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onStartFreeSession: () -> Unit = {},
    onDiscardWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MutantColors.Background)
            .padding(horizontal = MutantSpacing.md),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Top Bar & Status Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MutantSpacing.md, bottom = MutantSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Active Session",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantColors.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Workout Tracker",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MutantColors.TextMetadata
                    )
                )
            }

            // Status Chip
            Surface(
                shape = CircleShape,
                color = Color(0xFF1E1A29),
                border = BorderStroke(1.dp, Color(0xFF2E2742))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (hasEmptySession) Color(0xFFEF4444) else Color(0xFFF59E0B))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasEmptySession) "Session / No Exercises" else "Idle / No Session",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MutantColors.TextSecondary
                        )
                    )
                }
            }
        }

        // --- Main Hero Card (Empty State Container) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF161322),
                border = BorderStroke(1.dp, Color(0xFF282338))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dumbbell Icon Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF201B30))
                            .border(1.dp, Color(0xFF33294E), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (hasEmptySession) "NO EXERCISES IN WORKOUT" else "NO ACTIVE WORKOUT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = MutantColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (hasEmptySession) {
                            "This workout session has no exercises. Discard it and add exercises to your protocol."
                        } else {
                            "Select a scheduled day from your routine or kick off an ad-hoc workout session right now."
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MutantColors.TextMetadata,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    if (hasEmptySession) {
                        Button(
                            onClick = onDiscardWorkout,
                            enabled = !isDiscarding,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("discard_workout")
                        ) {
                            Text("DISCARD WORKOUT", fontWeight = FontWeight.Black)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = onNavigateToProtocols,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GO TO PROTOCOLS", color = MutantColors.TextSecondary)
                        }
                    } else {
                        // Primary CTA: GO TO PROTOCOLS
                        Button(
                            onClick = onNavigateToProtocols,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color(0xFF150826)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("go_to_protocols")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "GO TO PROTOCOLS",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary CTA: + Start Free Session
                        Surface(
                            onClick = onStartFreeSession,
                            shape = CircleShape,
                            color = Color(0xFF1A1728),
                            border = BorderStroke(1.dp, Color(0xFF2E2742)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("start_free_session")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ Start Free Session",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MutantColors.TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Bottom Card: Last Logged Workout ---
        val hasPreviousSession = !lastSessionTitle.isNullOrBlank() &&
            lastSessionTitle != "nenhum treino concluído"

        val timeAgoText = when {
            lastSessionDaysAgo == null || lastSessionDaysAgo < 0 -> ""
            lastSessionDaysAgo == 0 -> "Today"
            lastSessionDaysAgo == 1 -> "1 day ago"
            else -> "$lastSessionDaysAgo days ago"
        }

        val lastSummaryFormatted = if (hasPreviousSession) {
            if (lastSessionTitle!!.contains("·") || lastSessionTitle.contains("•")) {
                lastSessionTitle
            } else if (timeAgoText.isNotEmpty()) {
                "$lastSessionTitle • $timeAgoText"
            } else {
                lastSessionTitle
            }
        } else {
            "No workouts recorded yet"
        }

        Surface(
            onClick = onNavigateToHistory,
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161322),
            border = BorderStroke(1.dp, Color(0xFF282338)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MutantSpacing.md)
                .testTag("last_logged_workout_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF201B30))
                        .border(1.dp, Color(0xFF33294E), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LAST LOGGED WORKOUT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7),
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = lastSummaryFormatted,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MutantColors.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MutantColors.TextMetadata
                )
            }
        }
    }
}
