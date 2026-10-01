package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantMotion
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.MutantChoiceChip
import com.example.ui.designsystem.components.MutantEyebrow

/** Post-set feedback maps onto the quality values the progression engine reads. */
val FormOptions = listOf("Clean" to "Excellent", "OK" to "Good", "Broke down" to "Compromised")
val TargetOptions = listOf("Strong" to "Excellent", "Medium" to "Good", "Weak" to "Weak")

@Composable
fun RestTimerBar(
    remainingSeconds: Int,
    isRunning: Boolean,
    isComplete: Boolean,
    plannedSeconds: Int,
    executionQuality: String?,
    targetMuscleQuality: String?,
    onExecutionQuality: (String) -> Unit,
    onTargetMuscleQuality: (String) -> Unit,
    onAdjustTime: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val visible = remainingSeconds > 0 || isRunning || isComplete

    // Longest countdown seen in this rest period, so +30 grows the bar instead of overflowing it.
    var longest by remember { mutableIntStateOf(plannedSeconds) }
    LaunchedEffect(visible, remainingSeconds, plannedSeconds) {
        longest = if (!visible) plannedSeconds else maxOf(longest, remainingSeconds, 1)
    }
    var firedHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(isComplete) {
        if (isComplete && !firedHaptic) {
            firedHaptic = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (!isComplete) firedHaptic = false
    }

    val preferences = remember(context) {
        context.getSharedPreferences(RestTimerAlerts.PREFERENCES, android.content.Context.MODE_PRIVATE)
    }
    var alarmEnabled by remember { mutableStateOf(preferences.getBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, false)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        alarmEnabled = granted
        preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, granted).apply()
        if (granted) RestTimerAlerts.prepare(context)
    }

    val done = isComplete && remainingSeconds <= 0
    val paused = !isRunning && remainingSeconds > 0
    val accent = if (done) MutantColors.Success else MutantColors.Primary
    val fraction by animateFloatAsState(
        if (done) 0f else remainingSeconds / longest.coerceAtLeast(1).toFloat(),
        tween(1000), label = "RestFraction"
    )

    AnimatedVisibility(
        visible = visible,
        enter = MutantMotion.timerEnterTransition(density),
        exit = MutantMotion.timerExitTransition(density),
        modifier = modifier
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), ambientColor = androidx.compose.ui.graphics.Color.Black)
                .background(MutantColors.SurfaceContainerHigh, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)
                .testTag("rest_timer_bar"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(MutantColors.Outline, RoundedCornerShape(2.dp))) {
                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(accent, RoundedCornerShape(2.dp)))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MutantEyebrow(
                        when {
                            done -> "GO — NEXT SET"
                            paused -> "REST · PAUSED"
                            else -> "REST"
                        },
                        color = accent, style = MutantType.Eyebrow.copy(letterSpacing = 0.1.em)
                    )
                    val shown = remainingSeconds.coerceAtLeast(0)
                    Text("${shown / 60}:${(shown % 60).toString().padStart(2, '0')}", style = MutantType.MonoTimer,
                        color = MutantColors.TextPrimary, modifier = Modifier.testTag("rest_timer_time"))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    RoundTextButton("−15", "Remove 15 seconds", "rest_minus_15") { onAdjustTime(-15) }
                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        enabled = !done,
                        modifier = Modifier.size(56.dp).testTag("rest_toggle"),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MutantColors.Primary, contentColor = MutantColors.OnPrimary,
                            disabledContainerColor = MutantColors.Primary.copy(alpha = 0.3f), disabledContentColor = MutantColors.OnPrimary
                        )
                    ) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                            contentDescription = if (paused) "Resume rest" else "Pause rest", modifier = Modifier.size(28.dp))
                    }
                    RoundTextButton("+30", "Add 30 seconds", "rest_plus_30") { onAdjustTime(30) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackRow("FORM", FormOptions, executionQuality, "form", onExecutionQuality)
                FeedbackRow("TARGET", TargetOptions, targetMuscleQuality, "target", onTargetMuscleQuality)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = {
                        if (alarmEnabled) {
                            alarmEnabled = false
                            preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, false).apply()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            alarmEnabled = true
                            preferences.edit().putBoolean(RestTimerAlerts.SOUND_ENABLED_KEY, true).apply()
                            RestTimerAlerts.prepare(context)
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(36.dp).testTag("rest_timer_alarm_toggle")
                ) {
                    Icon(if (alarmEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null, tint = MutantColors.TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (alarmEnabled) "Sound + alert" else "Silent", style = MutantType.Caption.copy(fontWeight = FontWeight.Medium),
                        color = MutantColors.TextSecondary)
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.height(36.dp).testTag("rest_skip"),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MutantColors.Line, contentColor = MutantColors.TextPrimary)
                ) { Text(if (done) "Close" else "Skip rest", style = MutantType.Chip) }
            }
        }
    }
}

@Composable
private fun RoundTextButton(label: String, description: String, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp).testTag(tag),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(1.dp, MutantColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MutantColors.TextPrimary)
    ) {
        Text(label, style = MutantType.MonoLabel.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun FeedbackRow(
    label: String,
    options: List<Pair<String, String>>,
    selectedValue: String?,
    tag: String,
    onSelect: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MutantEyebrow(label, modifier = Modifier.width(62.dp), style = MutantType.Eyebrow.copy(letterSpacing = 0.sp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (text, value) ->
                MutantChoiceChip(
                    label = text,
                    selected = selectedValue.equals(value, ignoreCase = true),
                    onClick = { onSelect(value) },
                    modifier = Modifier.weight(1f).testTag("feedback_${tag}_$value"),
                    height = 34.dp, cornerRadius = 10.dp, horizontalPadding = 0.dp
                )
            }
        }
    }
}
