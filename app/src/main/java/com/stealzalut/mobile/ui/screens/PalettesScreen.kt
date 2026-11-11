package com.stealzalut.mobile.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.core.image.SampledBitmapLoader
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.ui.components.ImagePickerCard
import com.stealzalut.mobile.ui.components.PaletteSwatchesView
import com.stealzalut.mobile.ui.components.SzlEmptyState
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

@Composable
fun PalettesScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var presetName by remember { mutableStateOf("") }
    var presetNotes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSavedPresets()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(if (uiState.palettePhotoBitmap != null) AppDimens.SpacingSmall else AppDimens.SpacingSpacious)
    ) {
        // Section Title Banner matching Figma Node 12:1691
        com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner(
            title = stringResource(R.string.palettes_header_title),
            subtitle = stringResource(R.string.palettes_header_desc)
        )

        // Image Picker Card
        ImagePickerCard(
            title = stringResource(R.string.palettes_picker_title),
            subtitle = stringResource(R.string.palettes_picker_subtitle),
            badgeText = stringResource(R.string.wizard_step_2_ref_badge),
            bitmap = uiState.palettePhotoBitmap,
            previewHeight = AppDimens.ImagePickerCollapsedHeight,
            onImageSelected = { uri ->
                viewModel.setPalettePhoto(uri)
                if (presetName.isBlank()) {
                    presetName = "Preset ${System.currentTimeMillis() % 1000}"
                }
            }
        )

        // Palette Extraction Status / Results
        if (uiState.isExtractingPalette) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpacingHuge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))
                    Text(
                        text = stringResource(R.string.wizard_step_4_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else if (uiState.extractedPalette.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(AppDimens.SpacingNormal),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    PaletteSwatchesView(
                        title = stringResource(R.string.palettes_swatches_title),
                        colors = uiState.extractedPalette
                    )

                    // Streamlined Preset Action Form (Minimalist footprint)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                    ) {
                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text(stringResource(R.string.dialog_theme_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.saveExtractedPreset(presetName, presetNotes.ifBlank { null })
                                },
                                enabled = presetName.isNotBlank() && !uiState.isSavingPreset && !uiState.isExtractingPalette,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (uiState.isSavingPreset) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                                        strokeWidth = AppDimens.BorderThick,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = MaterialTheme.icons.actionSave,
                                        contentDescription = null,
                                        modifier = Modifier.size(AppDimens.IconNormal)
                                    )
                                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                    Text(
                                        text = stringResource(R.string.dialog_btn_save),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.exportAndShareCubeFromPaletteImage(context)
                                },
                                enabled = !uiState.isExportingCube && !uiState.isSavingPreset && !uiState.isExtractingPalette,
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                            ) {
                                if (uiState.isExportingCube) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(AppDimens.ProgressIndicatorSize),
                                        strokeWidth = AppDimens.BorderThick,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = MaterialTheme.icons.actionShare,
                                        contentDescription = null,
                                        modifier = Modifier.size(AppDimens.IconNormal)
                                    )
                                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                    Text(
                                        text = stringResource(R.string.palettes_btn_export_cube),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Saved Presets Library
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.SpacingStandard),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
            ) {
                Text(
                    text = stringResource(R.string.palettes_saved_section_title, uiState.savedPresets.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (uiState.savedPresets.isEmpty()) {
                    SzlEmptyState(
                        icon = MaterialTheme.icons.toolColorMixer,
                        title = stringResource(R.string.palettes_no_presets),
                        description = stringResource(R.string.wizard_step_2_no_saved)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
                        uiState.savedPresets.forEach { preset ->
                            PresetItemCard(
                                preset = preset,
                                onExportCube = {
                                    viewModel.exportAndShareCubeFromPreset(context, preset)
                                },
                                onDelete = { viewModel.deletePreset(preset.id) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSpacious))
    }
}

@Composable
private fun PresetItemCard(
    preset: ColorimetryPreset,
    onExportCube: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBitmap by produceState<Bitmap?>(initialValue = null, preset.thumbnailPath, preset.referenceImagePath) {
        value = withContext(Dispatchers.IO) {
            (preset.thumbnailPath ?: preset.referenceImagePath)?.let { path ->
                SampledBitmapLoader.decodeFile(File(path), maxDimension = 500)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.SpacingMedium),
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
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${stringResource(preset.method.displayNameRes)} • ${(preset.intensity * 100).toInt()}% • LUT ${preset.lutSize.size}³",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    preset.notes?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionDelete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Visual Color Spectrum Card (Mire des couleurs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.CalibrationCardHeight)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .background(ColorCardReference.NeutralDarkBackground)
                    .border(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(AppDimens.CornerRadiusNormal)),
                contentAlignment = Alignment.Center
            ) {
                cardBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.palettes_mire_content_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: run {
                    // Fallback visual calibration card layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppDimens.SpacingMedium),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(AppDimens.CalibrationRowHeightMedium), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(Color.White))
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(ColorCardReference.WhiteStep))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.CalibrationRowHeightSmall)
                                .clip(RoundedCornerShape(AppDimens.CornerRadiusTiny))
                                .background(
                                    Brush.horizontalGradient(ColorCardReference.NeutralRampFive)
                                )
                        )
                        Row(modifier = Modifier.fillMaxWidth().height(AppDimens.CalibrationRowHeightMicro), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(ColorCardReference.GrayMiddle1))
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(ColorCardReference.GrayMiddle2))
                        }
                        val patches = ColorCardReference.PrimaryCardPatches
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
                            for (p in patches) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(p))
                            }
                        }
                    }
                }
            }

            // Swatches
            if (preset.paletteHexes.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                    modifier = Modifier.padding(vertical = AppDimens.SpacingMicro)
                ) {
                    items(preset.paletteHexes) { hex ->
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
            }

            OutlinedButton(
                onClick = onExportCube,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.actionShare,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.palettes_btn_export_cube),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
