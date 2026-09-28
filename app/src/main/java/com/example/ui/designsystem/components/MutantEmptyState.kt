package com.example.ui.designsystem.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantPalette
import com.example.ui.designsystem.MutantSpacing

@Composable
fun MutantEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    MutantCard(
        modifier = modifier,
        variant = MutantCardVariant.CONTAINER,
        contentPadding = MutantSpacing.lg
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MutantPalette.PrimaryPurple,
                modifier = Modifier.size(40.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MutantPalette.TextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MutantPalette.TextSecondary,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center
            )

            if (actionButton != null) {
                Spacer(modifier = Modifier.height(MutantSpacing.xs))
                actionButton()
            }
        }
    }
}
