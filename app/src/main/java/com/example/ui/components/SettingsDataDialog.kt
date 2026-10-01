package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.MutantViewModel

@Composable
fun SettingsDataDialog(
    viewModel: MutantViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isExporting by viewModel.isExporting.collectAsState()
    val fileName = remember { viewModel.exportFileName() }
    val toast = LocalMutantToast.current

    MutantBottomSheet(onDismiss = onDismiss, dismissible = !isExporting, modifier = Modifier.testTag("settings_sheet")) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Settings", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
            MutantEyebrow("DATA", modifier = Modifier.padding(top = 6.dp),
                style = MutantType.Eyebrow.copy(fontSize = 10.5.sp, letterSpacing = 0.1.em))
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MutantColors.Background, RoundedCornerShape(18.dp))
                    .border(1.dp, MutantColors.OutlineVariant, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = MutantColors.Primary, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Export for analysis", style = MutantType.RowTitle.copy(fontSize = 15.sp), color = MutantColors.TextPrimary)
                        Text(
                            "Workouts, sets, program, bodyweight, cardio and recovery in one JSON file.",
                            style = MutantType.BodySmall, color = MutantColors.TextSecondary
                        )
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MutantColors.SurfaceContainer, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Code, contentDescription = null, tint = MutantColors.Warning, modifier = Modifier.size(16.dp))
                    Text(fileName, style = MutantType.MonoLabel.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Medium),
                        color = MutantColors.Warning)
                }
                MutantButton(
                    text = if (isExporting) "Building JSON…" else "Export & share",
                    onClick = {
                        viewModel.shareExportFile(context) {
                            toast.show("JSON ready · opening share sheet")
                            onDismiss()
                        }
                    },
                    icon = Icons.Outlined.Share,
                    loading = isExporting,
                    height = 52.dp,
                    textStyle = MutantType.Button.copy(fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().testTag("export_json_button")
                )
            }
        }
    }
}
