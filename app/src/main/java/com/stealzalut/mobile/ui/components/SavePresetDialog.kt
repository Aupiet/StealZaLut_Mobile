package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import kotlin.math.roundToInt

@Composable
fun SavePresetDialog(
    method: ColorTransferMethod,
    intensity: Float,
    lutSize: LutSize,
    paletteHexes: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String?) -> Unit
) {
    val defaultName = stringResource(R.string.wizard_step_2_ref_badge) + " " + (System.currentTimeMillis() % 1000)
    var name by remember { mutableStateOf(defaultName) }
    var notes by remember { mutableStateOf("") }

    val methodName = stringResource(method.displayNameRes)
    val lutLabel = stringResource(lutSize.labelRes)
    val percentage = (intensity * AppConstants.INTENSITY_PERCENT_MULTIPLIER).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_save_theme_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
            ) {
                Text(
                    text = stringResource(R.string.dialog_save_theme_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Recipe Summary Card
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                        Text(
                            text = "${stringResource(R.string.wizard_step_3_method_title)} : $methodName",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stringResource(R.string.wizard_step_3_intensity_title)} : $percentage% • $lutLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (paletteHexes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
                            PaletteSwatchesView(title = stringResource(R.string.palettes_swatches_title), colors = paletteHexes)
                        }
                    }
                }

                // Preset Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_theme_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = name
                        },
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                )

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.dialog_notes_label)) },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = notes
                        },
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, notes.takeIf { it.isNotBlank() }) },
                modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium)
            ) {
                Text(stringResource(R.string.dialog_btn_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinTouchTarget)
            ) {
                Text(stringResource(R.string.dialog_btn_cancel))
            }
        },
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge)
    )
}
