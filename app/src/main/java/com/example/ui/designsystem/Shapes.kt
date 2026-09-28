package com.example.ui.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MutantShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object MutantShapeTokens {
    val Hairline = RoundedCornerShape(1.dp)
    val Micro = RoundedCornerShape(2.dp)
    val Compact = RoundedCornerShape(4.dp)
    val SmallControl = RoundedCornerShape(6.dp)
    val TinyControl = RoundedCornerShape(8.dp)
    val CompactControl = RoundedCornerShape(10.dp)
    val InputChip = RoundedCornerShape(12.dp)
    val Selector = RoundedCornerShape(14.dp)
    val Panel = RoundedCornerShape(16.dp)
    val Card = RoundedCornerShape(18.dp)
    val LargePanel = RoundedCornerShape(20.dp)
    val MajorContainer = RoundedCornerShape(26.dp)
    val Button = RoundedCornerShape(14.dp)
}

object MutantStrokeWidths {
    val Hairline = 0.5.dp
    val Standard = 1.dp
    val Emphasized = 1.5.dp
}
