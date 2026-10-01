package com.example.ui.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType

@Composable
fun MutantTopBar(
    gymName: String? = null,
    gymTag: String = "A",
    onGymClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("MUTANT LOG", style = MutantType.Brand, color = MutantColors.Primary)
            MutantEyebrow("TRAINING OS", style = MutantType.Eyebrow.copy(fontSize = 9.5.sp, letterSpacing = 0.12.em))
        }
        Surface(
            onClick = onGymClick,
            modifier = Modifier.height(40.dp).widthIn(max = 180.dp).testTag("gym_selector_chip"),
            shape = RoundedCornerShape(20.dp),
            color = MutantColors.SurfaceContainer,
            border = BorderStroke(1.dp, MutantColors.Line)
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.size(20.dp).background(MutantColors.PrimarySelected, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(gymTag, style = MutantType.MonoLabel.copy(fontSize = 10.sp, lineHeight = 10.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = MutantColors.Primary)
                }
                Text(
                    gymName ?: "Pick gym", style = MutantType.Chip, color = MutantColors.TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Rounded.ExpandMore, contentDescription = "Change gym", tint = MutantColors.TextSecondary,
                    modifier = Modifier.size(18.dp))
            }
        }
        Surface(
            onClick = onSettingsClick,
            modifier = Modifier.size(40.dp).testTag("settings_button"),
            shape = CircleShape,
            color = MutantColors.SurfaceContainer,
            border = BorderStroke(1.dp, MutantColors.Line)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings and data export",
                    tint = MutantColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
