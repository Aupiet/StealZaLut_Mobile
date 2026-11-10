package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.ImagePickerCard

@Composable
fun StudioStep1Source(
    sourceBitmap: Bitmap?,
    onImageSelected: (Uri) -> Unit,
    canProceed: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    val rawPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onImageSelected(uri)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {


        ImagePickerCard(
            title = if (sourceBitmap != null) stringResource(R.string.wizard_step_1_ready) else stringResource(R.string.wizard_step_1_title),
            subtitle = if (sourceBitmap != null) stringResource(R.string.wizard_step_1_badge) else stringResource(R.string.wizard_step_1_empty),
            badgeText = stringResource(R.string.wizard_step_1_badge),
            bitmap = sourceBitmap,
            onImageSelected = onImageSelected
        )

        OutlinedButton(
            onClick = { rawPickerLauncher.launch("*/*") }, // GUARD_ALLOW
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Icon(
                imageVector = MaterialTheme.icons.actionImport,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Text(
                text = stringResource(R.string.btn_open_raw_dng),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (sourceBitmap != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                    border = BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ISO 100", // GUARD_ALLOW
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "f/1.8", // GUARD_ALLOW
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1/250s", // GUARD_ALLOW
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = com.stealzalut.mobile.ui.theme.LocalFontSpec.current.mono),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.actionRefresh,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_1_change),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Text(
                text = stringResource(R.string.wizard_step_1_btn_next),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Icon(
                imageVector = MaterialTheme.icons.statusArrowForward,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }
    }
}
