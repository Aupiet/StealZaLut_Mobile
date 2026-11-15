package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.icons

/**
 * Step 1: Source Image Selection (Prototype Mobile v2.3).
 * Strictly 0-scroll on all screen sizes & aspect ratios:
 * - Maximized flexible WYSIWYG card (weight = 1f).
 * - Direct tap on card to select or change photo.
 * - Single secondary action for RAW / DNG files.
 * - Zero fake metadata or redundant buttons.
 */
@Composable
fun StudioStep1Source(
    sourceBitmap: Bitmap?,
    onImageSelected: (Uri) -> Unit,
    canProceed: Boolean,
    onReset: () -> Unit = {},
    onNext: () -> Unit = {},
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
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        if (sourceBitmap != null) {
            // Maximized flexible WYSIWYG card (weight = 1f, strictly 0-scroll)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
                ),
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.SpacingSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = sourceBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.wizard_step_1_ready),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium)),
                        contentScale = ContentScale.Fit
                    )

                    // Subtle bottom pill indicating tap to change
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AppDimens.SpacingSmall)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Icon(
                                imageVector = MaterialTheme.icons.actionRefresh,
                                contentDescription = null,
                                modifier = Modifier.size(AppDimens.IconSmall),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.wizard_step_1_change),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Secondary option: Open RAW / DNG
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
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            // Welcoming empty drop zone (weight = 1f, strictly 0-scroll)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
                ),
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                border = BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimens.SpacingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(AppDimens.IconJumbo)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MaterialTheme.icons.actionAddPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconLarge),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.wizard_step_1_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.wizard_step_1_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Secondary option: Open RAW / DNG
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
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
