package com.example.ui.components

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens
import com.example.ui.designsystem.MutantPlateColors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PlateVisualizer(
    totalWeightKg: Float,
    barWeightKg: Float = 20f,
    modifier: Modifier = Modifier
) {
    // Calculate plates per side
    val weightPerSide = ((totalWeightKg - barWeightKg) / 2f).coerceAtLeast(0f)

    // Standard plates in kg
    val plates = listOf(
        25f to MutantPlateColors.Red,
        20f to MutantPlateColors.Blue,
        15f to MutantPlateColors.Yellow,
        10f to MutantPlateColors.Green,
        5f to MutantPlateColors.White,
        2.5f to MutantPlateColors.Slate,
        1.25f to MutantPlateColors.LightSlate
    )

    val sidePlates = mutableListOf<Color>()
    var remaining = weightPerSide

    for ((plateWeight, color) in plates) {
        while (remaining >= plateWeight) {
            sidePlates.add(color)
            remaining -= plateWeight
            if (sidePlates.size >= 6) break // Limit visual width
        }
        if (sidePlates.size >= 6) break
    }

    Row(
        modifier = modifier
            .height(28.dp)
            .padding(horizontal = MutantSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barbell sleeve
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(6.dp)
                .background(MutantPlateColors.LightSlate, MutantShapeTokens.Hairline)
        )
        // Collar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(MutantPlateColors.Collar, MutantShapeTokens.Hairline)
        )
        // Stack of plates
        if (sidePlates.isEmpty()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(14.dp)
                    .background(MutantPlateColors.DarkSlate, MutantShapeTokens.Hairline)
            )
        } else {
            sidePlates.forEach { color ->
                Spacer(modifier = Modifier.width(1.5.dp))
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(24.dp)
                        .clip(MutantShapeTokens.Hairline)
                        .background(color)
                )
            }
        }
        // Shaft extension
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(4.dp)
                .background(MutantPlateColors.Slate, MutantShapeTokens.Hairline)
        )
    }
}
