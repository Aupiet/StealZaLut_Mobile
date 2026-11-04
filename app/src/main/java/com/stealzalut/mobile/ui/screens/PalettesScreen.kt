package com.stealzalut.mobile.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.ui.components.ImagePickerCard
import com.stealzalut.mobile.ui.components.PaletteSwatchesView
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

@Composable
fun PalettesScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSpacious)
    ) {
        // Section Header
        Surface(
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.palettes_header_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                Text(
                    text = stringResource(R.string.palettes_header_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Image Picker Card
        ImagePickerCard(
            title = stringResource(R.string.palettes_picker_title),
            subtitle = stringResource(R.string.palettes_picker_subtitle),
            badgeText = stringResource(R.string.wizard_step_2_ref_badge),
            bitmap = uiState.palettePhotoBitmap,
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
                    modifier = Modifier.padding(AppDimens.SpacingStandard),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)
                ) {
                    PaletteSwatchesView(
                        title = stringResource(R.string.palettes_swatches_title),
                        colors = uiState.extractedPalette
                    )

                    // Save as Preset Form
                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_save_theme_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = presetName,
                            onValueChange = { presetName = it },
                            label = { Text(stringResource(R.string.dialog_theme_name_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                        )

                        OutlinedTextField(
                            value = presetNotes,
                            onValueChange = { presetNotes = it },
                            label = { Text(stringResource(R.string.dialog_notes_label)) },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                        )

                        Button(
                            onClick = {
                                viewModel.saveExtractedPreset(presetName, presetNotes.ifBlank { null })
                            },
                            enabled = presetName.isNotBlank() && !uiState.isSavingPreset && !uiState.isExtractingPalette,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
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
                                    imageVector = Icons.Default.Save,
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
                    Text(
                        text = stringResource(R.string.palettes_no_presets),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)) {
                        uiState.savedPresets.forEach { preset ->
                            PresetItemCard(
                                preset = preset,
                                onApply = { viewModel.applyPresetToStudio(preset) },
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
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBitmap by produceState<Bitmap?>(initialValue = null, preset.thumbnailPath, preset.referenceImagePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val targetPath = preset.thumbnailPath ?: preset.referenceImagePath
                if (targetPath != null) {
                    val file = File(targetPath)
                    if (file.exists()) {
                        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeFile(file.absolutePath, opts)
                        var sample = 1
                        val maxDim = maxOf(opts.outWidth, opts.outHeight)
                        while (maxDim / (sample * 2) >= 500) {
                            sample *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                        BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    } else null
                } else null
            } catch (_: Exception) {
                null
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
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Supprimer",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Visual Color Spectrum Card (Mire des couleurs)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .background(Color(0xFF202228))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(AppDimens.CornerRadiusNormal)),
                contentAlignment = Alignment.Center
            ) {
                if (cardBitmap != null) {
                    Image(
                        bitmap = cardBitmap!!.asImageBitmap(),
                        contentDescription = "Mire des couleurs",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Fallback visual calibration card layout
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(Color.White))
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(Color(0xFFF0F0F0)))
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Black, Color(0xFF333333), Color(0xFF666666), Color(0xFF999999), Color.White)
                                    )
                                )
                        )
                        Row(modifier = Modifier.fillMaxWidth().height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(Color(0xFF767676)))
                            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(Color(0xFF808080)))
                        }
                        val patches = listOf(
                            Color(0xFFCD9B84), Color(0xFF9B6E55), Color(0xFF5F3E2D),
                            Color(0xFFE12D2D), Color(0xFF2DB937), Color(0xFF2D55E1)
                        )
                        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (p in patches) {
                                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(p))
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

            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.palettes_btn_apply),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
