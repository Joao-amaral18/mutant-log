package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.MutantButton
import com.example.ui.designsystem.components.MutantButtonStyle
import com.example.ui.designsystem.components.MutantEyebrow

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
    modifier: Modifier = Modifier,
    plannedWorkoutTitle: String? = null,
    onStartPlanned: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MutantColors.Background)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Session", style = MutantType.ScreenTitle, color = MutantColors.TextPrimary)
            Row(
                Modifier
                    .height(30.dp)
                    .background(MutantColors.SurfaceContainer, RoundedCornerShape(15.dp))
                    .border(1.dp, MutantColors.Line, RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).background(if (hasEmptySession) MutantColors.Error else MutantColors.TextMetadata, CircleShape))
                MutantEyebrow(if (hasEmptySession) "EMPTY" else "IDLE", color = MutantColors.TextSecondary,
                    style = MutantType.MonoLabel.copy(letterSpacing = 0.sp))
            }
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).padding(vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
        ) {
            Text(
                if (hasEmptySession) "This session has no exercises." else "No workout running.",
                style = MutantType.DisplayLarge, color = MutantColors.TextPrimary
            )
            Text(
                if (hasEmptySession) {
                    buildAnnotatedString { append("Discard it, then add exercises to the day in Protocol.") }
                } else if (plannedWorkoutTitle != null) {
                    buildAnnotatedString {
                        append("Today’s plan is ")
                        withStyle(SpanStyle(color = MutantColors.TextPrimary, fontWeight = FontWeight.SemiBold)) { append(plannedWorkoutTitle) }
                        append(". Start it, or log a free session.")
                    }
                } else {
                    buildAnnotatedString { append("Pick a day in Protocol, or log a free session.") }
                },
                style = MutantType.Body.copy(fontSize = 15.sp, lineHeight = 22.sp), color = MutantColors.TextSecondary
            )
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (hasEmptySession) {
                    MutantButton(
                        "Discard session", onClick = onDiscardWorkout, style = MutantButtonStyle.Danger,
                        icon = Icons.Rounded.DeleteOutline, enabled = !isDiscarding,
                        modifier = Modifier.fillMaxWidth().testTag("discard_workout")
                    )
                    MutantButton(
                        "Go to Protocol", onClick = onNavigateToProtocols, style = MutantButtonStyle.Surface, height = 54.dp,
                        modifier = Modifier.fillMaxWidth().testTag("go_to_protocols")
                    )
                } else {
                    if (onStartPlanned != null && plannedWorkoutTitle != null) {
                        MutantButton(
                            "Start $plannedWorkoutTitle", onClick = onStartPlanned, icon = Icons.Rounded.PlayArrow,
                            modifier = Modifier.fillMaxWidth().testTag("start_planned_workout")
                        )
                    } else {
                        MutantButton(
                            "Go to Protocol", onClick = onNavigateToProtocols,
                            modifier = Modifier.fillMaxWidth().testTag("go_to_protocols")
                        )
                    }
                    MutantButton(
                        "Free session", onClick = onStartFreeSession, style = MutantButtonStyle.Surface,
                        icon = Icons.Rounded.Add, height = 54.dp,
                        modifier = Modifier.fillMaxWidth().testTag("start_free_session")
                    )
                }
            }
        }

        val hasPreviousSession = !lastSessionTitle.isNullOrBlank()
        val lastSummary = if (hasPreviousSession) {
            listOfNotNull(lastSessionTitle, relativeDays(lastSessionDaysAgo).ifBlank { null }).joinToString(" · ")
        } else "No workouts logged yet"
        Surface(
            onClick = onNavigateToHistory,
            shape = RoundedCornerShape(18.dp),
            color = MutantColors.SurfaceContainer,
            border = BorderStroke(1.dp, MutantColors.OutlineVariant),
            modifier = Modifier.fillMaxWidth().testTag("last_logged_workout_card")
        ) {
            Row(
                Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = MutantColors.Primary, modifier = Modifier.size(22.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    MutantEyebrow("LAST LOGGED")
                    Text(lastSummary, style = MutantType.RowTitle.copy(fontSize = 14.sp), color = MutantColors.TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MutantColors.TextMetadata, modifier = Modifier.size(22.dp))
            }
        }
    }
}
