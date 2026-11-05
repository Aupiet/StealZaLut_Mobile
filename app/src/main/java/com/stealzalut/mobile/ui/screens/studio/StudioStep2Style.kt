package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.ui.components.ImagePickerCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.stealzalut.mobile.ui.components.PaletteSwatchesView
import com.stealzalut.mobile.ui.components.SzlEmptyState

@Composable
fun StudioStep2Style(
    sourceMode: Int,
    onSourceModeChanged: (Int) -> Unit,
    customReferenceBitmap: Bitmap?,
    customReferencePalette: List<String>,
    onCustomRefSelected: (Uri) -> Unit,
    savedPresets: List<ColorimetryPreset>,
    selectedTheme: ColorimetryPreset?,
    onThemeSelected: (ColorimetryPreset) -> Unit,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_2_title),
            subtitle = stringResource(R.string.wizard_step_2_subtitle)
        )

        PrimaryTabRow(
            selectedTabIndex = sourceMode,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
        ) {
            Tab(
                selected = sourceMode == 0,
                onClick = { onSourceModeChanged(0) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.wizard_step_2_tab_photo_b),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
            Tab(
                selected = sourceMode == 1,
                onClick = { onSourceModeChanged(1) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = "${stringResource(R.string.wizard_step_2_tab_saved)} (${savedPresets.size})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }

        if (sourceMode == 0) {
            ImagePickerCard(
                title = stringResource(R.string.wizard_step_2_tab_photo_b),
                subtitle = stringResource(R.string.wizard_step_2_ref_empty),
                badgeText = stringResource(R.string.wizard_step_2_ref_badge),
                bitmap = customReferenceBitmap,
                onImageSelected = onCustomRefSelected
            )

            if (customReferencePalette.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                        PaletteSwatchesView(
                            title = stringResource(R.string.palettes_swatches_title),
                            colors = customReferencePalette
                        )
                    }
                }
            }
        } else {
            if (savedPresets.isEmpty()) {
                SzlEmptyState(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.wizard_step_2_tab_saved),
                    description = stringResource(R.string.wizard_step_2_no_saved)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(savedPresets, key = { it.id }) { preset ->
                        StudioPresetCard(
                            preset = preset,
                            isSelected = selectedTheme?.id == preset.id,
                            onSelect = { onThemeSelected(preset) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_2_btn_back),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .weight(1.3f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Text(
                    text = stringResource(R.string.wizard_step_2_btn_next),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
        }
    }
}

@Composable
private fun StudioPresetCard(
    preset: ColorimetryPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = preset.id) {
        value = withContext(Dispatchers.IO) {
            try {
                val targetPath = preset.thumbnailPath ?: preset.referenceImagePath
                if (targetPath != null) {
                    val file = File(targetPath)
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                        if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
                        var sample = 1
                        val maxDim = maxOf(opts.outWidth, opts.outHeight)
                        while (maxDim / (sample * 2) >= 300) {
                            sample *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    } else null
                } else null
            } catch (t: Throwable) {
                null
            }
        }
    }

    Card(
        onClick = onSelect,
        modifier = modifier.width(AppDimens.ImagePickerHeight),
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Column(modifier = Modifier.padding(AppDimens.SpacingSmall)) {
            // Thumbnail or Fallback Swatches
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.ThumbnailCardSize)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = preset.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (preset.paletteHexes.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall),
                        modifier = Modifier.padding(AppDimens.SpacingExtraSmall)
                    ) {
                        items(preset.paletteHexes.take(AppConstants.DEFAULT_PALETTE_COLOR_COUNT)) { hex ->
                            val color = try {
                                val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
                                Color(android.graphics.Color.parseColor(cleanHex))
                            } catch (_: Exception) {
                                Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(AppDimens.SwatchSize)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))

            Text(
                text = "${stringResource(preset.method.displayNameRes)} • ${(preset.intensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
