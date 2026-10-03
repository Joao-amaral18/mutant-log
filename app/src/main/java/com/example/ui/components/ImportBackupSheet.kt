package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ImportMode
import com.example.data.repository.ImportPreview
import com.example.data.repository.ImportResult
import com.example.ui.designsystem.MutantColors
import com.example.ui.designsystem.MutantType
import com.example.ui.designsystem.components.*
import com.example.ui.viewmodel.ImportUiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Preview, confirm and result of restoring a JSON export. Not dismissible while the import runs. */
@Composable
fun ImportBackupSheet(
    state: ImportUiState,
    onConfirm: (ImportMode) -> Unit,
    onDismiss: () -> Unit,
    onShareBackup: (File) -> Unit
) {
    if (state == ImportUiState.Idle) return
    MutantBottomSheet(
        onDismiss = onDismiss,
        dismissible = state !is ImportUiState.Running,
        modifier = Modifier.testTag("import_sheet")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (state) {
                ImportUiState.Reading -> Busy("Reading file…")
                is ImportUiState.Running -> Busy(if (state.mode == ImportMode.REPLACE) "Restoring…" else "Merging…")
                is ImportUiState.Preview -> PreviewContent(state.preview, onConfirm, onDismiss)
                is ImportUiState.Done -> DoneContent(state.result, state.mode, state.safetyBackup, onDismiss, onShareBackup)
                is ImportUiState.Error -> {
                    Text("Can't import", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
                    Notice(Icons.Rounded.Error, MutantColors.Error, state.message, Modifier.testTag("import_error"))
                    MutantButton("Close", onClick = onDismiss, style = MutantButtonStyle.Outline, height = 52.dp,
                        modifier = Modifier.fillMaxWidth())
                }
                ImportUiState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun Busy(label: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag("import_busy"),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = MutantColors.Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        Text(label, style = MutantType.Body, color = MutantColors.TextSecondary)
    }
}

@Composable
private fun PreviewContent(preview: ImportPreview, onConfirm: (ImportMode) -> Unit, onCancel: () -> Unit) {
    var mode by remember { mutableStateOf(ImportMode.MERGE) }
    val replace = mode == ImportMode.REPLACE
    Text("Import backup", style = MutantType.SheetTitle, color = MutantColors.TextPrimary)
    Text(
        listOfNotNull(
            "Exported ${preview.generatedAt.take(10)}",
            dateRange(preview.firstWorkoutAt, preview.lastWorkoutAt)
        ).joinToString(" · "),
        style = MutantType.BodySmall, color = MutantColors.TextSecondary, modifier = Modifier.offset(y = (-6).dp)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MutantStatWell("WORKOUTS", "${preview.workouts}", Modifier.weight(1f))
        MutantStatWell("SETS", "${preview.sets}", Modifier.weight(1f))
        MutantStatWell("CARDIO", "${preview.cardio}", Modifier.weight(1f))
    }
    preview.warnings.forEach { Notice(Icons.Outlined.Info, MutantColors.Warning, it, Modifier.testTag("import_warning")) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MutantEyebrow("MODE", modifier = Modifier.padding(start = 4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MutantChoiceChip("Merge", !replace, { mode = ImportMode.MERGE }, height = 44.dp, cornerRadius = 12.dp,
                modifier = Modifier.weight(1f).testTag("import_mode_merge"))
            MutantChoiceChip("Replace", replace, { mode = ImportMode.REPLACE }, height = 44.dp, cornerRadius = 12.dp,
                modifier = Modifier.weight(1f).testTag("import_mode_replace"))
        }
        Text(
            if (replace) "Restores the file exactly." else "Adds workouts and cardio you don't have yet. Nothing is deleted.",
            style = MutantType.BodySmall, color = MutantColors.TextSecondary, modifier = Modifier.padding(start = 4.dp)
        )
    }
    if (replace) {
        Notice(Icons.Rounded.Warning, MutantColors.Error,
            "Deletes all workouts, programs and gyms on this phone first. A safety backup is saved.",
            Modifier.testTag("import_replace_warning"))
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MutantButton("Cancel", onClick = onCancel, style = MutantButtonStyle.Outline, height = 52.dp,
            modifier = Modifier.weight(1f))
        MutantButton(
            if (replace) "Replace data" else "Merge",
            onClick = { onConfirm(mode) },
            style = if (replace) MutantButtonStyle.Danger else MutantButtonStyle.Primary,
            height = 52.dp,
            modifier = Modifier.weight(2f).testTag("import_confirm_button")
        )
    }
}

@Composable
private fun DoneContent(result: ImportResult, mode: ImportMode, safetyBackup: File?, onClose: () -> Unit, onShareBackup: (File) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MutantColors.Success, modifier = Modifier.size(24.dp))
        Text(if (mode == ImportMode.REPLACE) "Data restored" else "Import complete", style = MutantType.SheetTitle,
            color = MutantColors.TextPrimary)
    }
    Text(importSummary(result), style = MutantType.Body.copy(lineHeight = 20.sp), color = MutantColors.TextSecondary,
        modifier = Modifier.testTag("import_summary"))
    if (safetyBackup != null) {
        MutantButton("Share safety backup", onClick = { onShareBackup(safetyBackup) }, style = MutantButtonStyle.Outline,
            icon = Icons.Outlined.Share, height = 48.dp, textStyle = MutantType.ButtonSmall,
            modifier = Modifier.fillMaxWidth().testTag("import_share_backup"))
    }
    MutantButton("Done", onClick = onClose, height = 52.dp, modifier = Modifier.fillMaxWidth().testTag("import_done"))
}

@Composable
private fun Notice(icon: ImageVector, tint: Color, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MutantType.BodySmall, color = MutantColors.TextPrimary)
    }
}

internal fun importSummary(r: ImportResult): String = buildList {
    add("${r.addedWorkouts} workout${plural(r.addedWorkouts)} and ${r.addedSets} set${plural(r.addedSets)} added")
    if (r.addedCardio > 0) add("${r.addedCardio} cardio session${plural(r.addedCardio)} added")
    if (r.addedExercises > 0) add("${r.addedExercises} new exercise${plural(r.addedExercises)}")
    if (r.addedPrograms > 0) add("${r.addedPrograms} program${plural(r.addedPrograms)}")
    val skipped = r.skippedWorkouts + r.skippedCardio
    if (skipped > 0) add("$skipped already on this phone, skipped")
    if (r.invalidRows > 0) add("${r.invalidRows} invalid row${plural(r.invalidRows)} ignored")
}.joinToString(" · ")

private fun plural(n: Int) = if (n == 1) "" else "s"

private fun dateRange(first: Long?, last: Long?): String? {
    if (first == null || last == null) return null
    val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return if (fmt.format(Date(first)) == fmt.format(Date(last))) fmt.format(Date(first))
    else "${fmt.format(Date(first))} – ${fmt.format(Date(last))}"
}
