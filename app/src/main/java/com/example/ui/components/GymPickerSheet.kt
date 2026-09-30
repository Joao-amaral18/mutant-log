package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Gym
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.MutantBottomSheet

/** Single-letter badge for a gym: A, B, C… in list order. */
fun gymTag(index: Int): String = if (index in 0..25) ('A' + index).toString() else "#"

@Composable
fun GymPickerSheet(
    gyms: List<Gym>,
    selectedGymId: Long?,
    onSelect: (Gym) -> Unit,
    onDismiss: () -> Unit
) {
    MutantBottomSheet(onDismiss = onDismiss, modifier = Modifier.testTag("gym_picker_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Gym", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            Text(
                "Loads and machine setups follow the gym you pick.",
                style = MutantType.BodySmall, color = MutantColors.TextSecondary,
                modifier = Modifier.offset(y = (-6).dp)
            )
            gyms.forEachIndexed { index, gym ->
                val selected = gym.id == selectedGymId
                val accent = if (selected) MutantColors.Primary else MutantColors.TextSecondary
                Surface(
                    onClick = { onSelect(gym) },
                    modifier = Modifier.fillMaxWidth().testTag("gym_option_${gym.id}"),
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MutantColors.PrimarySelected else MutantColors.Background,
                    border = BorderStroke(1.dp, if (selected) MutantColors.Primary else MutantColors.OutlineVariant)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(40.dp).background(MutantColors.SurfaceContainerHigh, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(gymTag(index), style = MutantType.MonoValue.copy(fontSize = 14.sp, lineHeight = 16.sp), color = accent)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(gym.name, style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                            if (gym.notes.isNotBlank()) {
                                Text(gym.notes, style = MutantType.Caption, color = MutantColors.TextSecondary)
                            }
                        }
                        Icon(
                            if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (selected) "Selected" else null,
                            tint = accent, modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
