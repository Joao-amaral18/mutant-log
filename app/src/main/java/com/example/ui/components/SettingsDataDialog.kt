package com.example.ui.components

import com.example.ui.designsystem.MutantStrokeWidths

import com.example.ui.designsystem.MutantTracking

import com.example.ui.designsystem.MutantSpacing

import com.example.ui.designsystem.MutantShapeTokens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MutantViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsDataDialog(
    viewModel: MutantViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsState()
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(Date())
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MutantVolt,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MutantVolt,
                        letterSpacing = MutantTracking.Compact
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MutantSpacing.md)
            ) {
                // Section Breadcrumb: Settings -> Data -> Exportar dados para análise
                Surface(
                    shape = MutantShapeTokens.SmallControl,
                    color = MutantDarkNavy,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MutantSpacing.compactMd, vertical = MutantSpacing.compact),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MutantTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Data",
                            style = MaterialTheme.typography.labelSmall.copy(color = MutantTextSecondary)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MutantTextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Exportar dados para análise",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MutantVolt,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Export Card
                Surface(
                    shape = MutantShapeTokens.CompactControl,
                    color = MutantDarkNavy,
                    border = androidx.compose.foundation.BorderStroke(MutantStrokeWidths.Standard, MutantBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(MutantSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(MutantSpacing.sm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MutantSpacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MutantVolt,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "EXPORTAR DADOS",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MutantTextPrimary,
                                    letterSpacing = MutantTracking.Compact
                                )
                            )
                        }

                        Text(
                            text = "Gera um arquivo JSON com treinos, séries, programa, peso corporal, cardio e recuperação.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MutantTextSecondary,
                                lineHeight = 18.sp
                            )
                        )

                        // File name preview
                        Surface(
                            shape = MutantShapeTokens.SmallControl,
                            color = MutantSurfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = MutantSpacing.xs, vertical = MutantSpacing.compact),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(MutantSpacing.compact)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = MutantAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "mutant-log-analysis-$todayDateStr.json",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MutantAmber,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MutantSpacing.xxs))

                        // Button: EXPORTAR JSON
                        Button(
                            onClick = {
                                viewModel.shareExportFile(context) {
                                    onDismiss()
                                }
                            },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutantVolt,
                                contentColor = MutantOnVolt,
                                disabledContainerColor = MutantVolt.copy(alpha = 0.5f),
                                disabledContentColor = MutantOnVolt.copy(alpha = 0.7f)
                            ),
                            shape = MutantShapeTokens.TinyControl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("export_json_button")
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MutantOnVolt,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(MutantSpacing.xs))
                                Text("GERANDO JSON...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(MutantSpacing.xs))
                                Text(
                                    text = "EXPORTAR JSON",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = MutantTracking.Compact
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting,
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text(
                    text = "FECHAR",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MutantTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        containerColor = MutantSurfaceCard,
        shape = MutantShapeTokens.InputChip
    )
}
