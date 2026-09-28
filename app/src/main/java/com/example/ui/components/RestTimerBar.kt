package com.example.ui.components

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RestTimerBar(
    remainingSeconds: Int,
    isRunning: Boolean,
    isComplete: Boolean,
    recommendedText: String,
    exerciseName: String,
    nextSetText: String,
    onAdjustTime: (Int) -> Unit, // e.g. -15 or +30
    onTogglePlayPause: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var hasFiredHaptic by remember { mutableStateOf(false) }
    var hasPublishedTimerNotification by remember { mutableStateOf(false) }
    var restCompleteState by remember { mutableStateOf<String?>(null) }
    var previousRemainingSeconds by remember { mutableIntStateOf(remainingSeconds) }
    var previousIsRunning by remember { mutableStateOf(isRunning) }
    var previousExerciseName by remember { mutableStateOf(exerciseName) }
    var previousSetText by remember { mutableStateOf(nextSetText) }
    val preferences = remember(context) {
        context.getSharedPreferences(RestTimerAlerts.PREFERENCES, android.content.Context.MODE_PRIVATE)
    }
    var alarmEnabled by remember {
        mutableStateOf(preferences.getBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, false))
    }
    var previousAlarmEnabled by remember { mutableStateOf(alarmEnabled) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        alarmEnabled = granted
        preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, granted).apply()
        if (granted) RestTimerAlerts.prepare(context)
    }
    val isVisible = remainingSeconds > 0 || isRunning || restCompleteState != null

    LaunchedEffect(remainingSeconds, isRunning, isComplete, alarmEnabled, exerciseName, nextSetText) {
        if (isComplete && !hasFiredHaptic) {
            hasFiredHaptic = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            restCompleteState = "REST COMPLETE"
            delay(1500)
            restCompleteState = "READY"
        } else if (!isComplete && remainingSeconds > 0) {
            hasFiredHaptic = false
            restCompleteState = null
        }
    }

    val minutes = (remainingSeconds / 60).coerceAtLeast(0)
    val seconds = (remainingSeconds % 60).coerceAtLeast(0)
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val isComplete = restCompleteState != null

    val borderColor by animateColorAsState(
        targetValue = when {
            isComplete -> MutantVolt
            isRunning -> MutantVolt.copy(alpha = 0.6f)
            else -> MutantBorder
        },
        animationSpec = tween(MotionDuration.Feedback),
        label = "TimerBorderColor"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = MutantMotion.timerEnterTransition(density),
        exit = MutantMotion.timerExitTransition(density)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("rest_timer_bar"),
            color = MutantSurfaceCard,
            shape = MutantShapeTokens.Selector,
            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, borderColor),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MutantSpacing.mdPlus, vertical = MutantSpacing.compactMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(MutantShapeTokens.Compact)
                                .background(if (isComplete || isRunning) MutantVolt else MutantAmber)
                        )
                        Spacer(modifier = Modifier.width(MutantSpacing.compact))
                        Text(
                            text = restCompleteState ?: "REST",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = MutantVolt,
                                letterSpacing = MutantTracking.Section
                            )
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Recomendado $recommendedText",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutantTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(modifier = Modifier.width(MutantSpacing.compact))
                        IconButton(
                            onClick = {
                                if (alarmEnabled) {
                                    alarmEnabled = false
                                    preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, false).apply()
                                } else if (
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    alarmEnabled = true
                                    preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, true).apply()
                                    RestTimerAlerts.prepare(context)
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("rest_timer_alarm_toggle")
                        ) {
                            Icon(
                                imageVector = if (alarmEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = if (alarmEnabled) "Disable rest timer alarm" else "Enable rest timer alarm",
                                tint = if (alarmEnabled) MutantVolt else MutantTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onSkip, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Skip timer", tint = MutantTextMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MutantSpacing.compact))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Time Display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onTogglePlayPause() }
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                            contentDescription = "Timer status",
                            tint = if (isComplete) MutantVolt else if (isRunning) MutantVolt else MutantAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(MutantSpacing.compact))
                        Text(
                            text = if (isComplete) "00:00" else timeFormatted,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isComplete) MutantVolt else MutantTextPrimary,
                                letterSpacing = MutantTracking.Compact
                            )
                        )
                    }

                    // Quick adjustments: [-15s] and [+30s]
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onAdjustTime(-15) },
                            contentPadding = PaddingValues(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.xxs),
                            shape = MutantShapeTokens.TinyControl,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantTextSecondary),
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("-15s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = { onAdjustTime(30) },
                            contentPadding = PaddingValues(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.xxs),
                            shape = MutantShapeTokens.TinyControl,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantVolt),
                            border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantVolt.copy(alpha = 0.6f)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("+30s", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
