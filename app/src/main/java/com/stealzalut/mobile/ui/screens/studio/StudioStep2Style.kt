package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.ui.components.PaletteSwatchesView
import com.stealzalut.mobile.ui.components.SzlEmptyState
import com.stealzalut.mobile.ui.theme.icons

/**
 * Step 2: Reference Style Selection (Prototype Mobile v2.3).
 * Strictly 0-scroll on all screen sizes & aspect ratios:
 * - Compact top tab bar.
 * - Mode 1: Maximized flexible reference image (weight = 1f) with compact 7-color ribbon.
 * - Mode 0: Scrollable cinema preset list (internal scroll only, outer layout remains pinned).
 * - Zero superfluous text or redundant badges.
 */
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
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onCustomRefSelected(uri)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. Compact Top Tab Row ---
        PrimaryTabRow(
            selectedTabIndex = sourceMode,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
        ) {
            Tab(
                selected = sourceMode == 0,
                onClick = { onSourceModeChanged(0) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.toolColorMixer,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.wizard_step_2_tab_saved),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
            Tab(
                selected = sourceMode == 1,
                onClick = { onSourceModeChanged(1) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = MaterialTheme.icons.actionAddPhoto,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = stringResource(R.string.wizard_step_2_tab_photo_b),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        }

        // --- 2. Content Section (Strictly Zero-Scroll) ---
        if (sourceMode == 0) {
            if (savedPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SzlEmptyState(
                        icon = MaterialTheme.icons.toolColorMixer,
                        title = stringResource(R.string.wizard_step_2_tab_saved),
                        description = stringResource(R.string.wizard_step_2_no_saved)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    items(savedPresets) { preset ->
                        StudioCinemaPresetCard(
                            preset = preset,
                            isSelected = selectedTheme?.id == preset.id,
                            onSelect = { onThemeSelected(preset) }
                        )
                    }
                }
            }
        } else {
            if (customReferenceBitmap != null) {
                // Maximized flexible reference card (weight = 1f, strictly 0-scroll)
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
                            bitmap = customReferenceBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.wizard_step_2_tab_photo_b),
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
                                    text = stringResource(R.string.wizard_step_2_change),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Dominant colors palette (Pantone / ShotDeck style)
                if (customReferencePalette.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                        border = BorderStroke(
                            AppDimens.BorderThin,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        ),
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
                                text = stringResource(R.string.wizard_step_2_tab_photo_b),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = stringResource(R.string.wizard_step_2_ref_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioCinemaPresetCard(
    preset: ColorimetryPreset,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    preset.notes?.let { notes ->
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = MaterialTheme.icons.statusCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(AppDimens.IconMicro)
                            )
                        }
                    }
                }
            }

            if (preset.paletteHexes.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    preset.paletteHexes.take(6).forEach { hex ->
                        val color = try {
                            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
                            Color(android.graphics.Color.parseColor(cleanHex))
                        } catch (_: Throwable) {
                            Color.Gray
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(AppDimens.CurtainHandleSize)
                                .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
                                .background(color)
                                .border(
                                    AppDimens.BorderThin,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                                    RoundedCornerShape(AppDimens.CornerRadiusMedium)
                                )
                        )
                    }
                }
            }
        }
    }
}
