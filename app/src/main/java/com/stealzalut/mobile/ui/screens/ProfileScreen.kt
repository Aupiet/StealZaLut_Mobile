package com.stealzalut.mobile.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import com.stealzalut.mobile.ui.components.SzlBottomGradientScrim
import com.stealzalut.mobile.ui.components.SzlEmptyState
import com.stealzalut.mobile.ui.theme.ColorCardReference
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorimetryPreset
import com.stealzalut.mobile.data.model.SavedGradedImage
import com.stealzalut.mobile.ui.components.ImageDetailDialog
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context: Context = LocalContext.current

    // SAF Document Creator launcher for exporting .cube LUT files
    val createCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { destinationUri ->
        if (destinationUri != null) {
            viewModel.saveExportedCubeToUri(destinationUri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedImages()
        viewModel.loadSavedPresets()
    }

    // Modal Image Detail Dialog
    uiState.selectedDetailImage?.let { detailImage ->
        ImageDetailDialog(
            image = detailImage,
            onDismiss = { viewModel.dismissImageDetail() },
            onApplyLook = { viewModel.applyLookFromSavedImage(it) },
            onShare = {
                val intent = viewModel.createShareImageIntent(it.filePath)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.detail_btn_share)))
            },
            onDelete = {
                viewModel.deleteSavedImage(it.id)
                viewModel.dismissImageDetail()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // Profile Header
        Surface(
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(AppDimens.SpacingNormal)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.profile_header_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                Text(
                    text = stringResource(R.string.profile_header_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Folder Tabs (Photos Instagram Grid vs Thèmes & Presets)
        PrimaryTabRow(
            selectedTabIndex = uiState.profileSelectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = uiState.profileSelectedTab == 0,
                onClick = { viewModel.setProfileTab(0) },
                text = {
                    Text(
                        text = stringResource(R.string.profile_tab_photos, uiState.savedImages.size),
                        fontWeight = if (uiState.profileSelectedTab == 0) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(imageVector = Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
                }
            )
            Tab(
                selected = uiState.profileSelectedTab == 1,
                onClick = { viewModel.setProfileTab(1) },
                text = {
                    Text(
                        text = stringResource(R.string.profile_tab_themes, uiState.savedPresets.size),
                        fontWeight = if (uiState.profileSelectedTab == 1) FontWeight.Bold else FontWeight.Normal
                    )
                },
                icon = {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(AppDimens.IconSmall))
                }
            )
        }

        // Tab 0: Photos - Instagram Style 3-Column Square Grid
        if (uiState.profileSelectedTab == 0) {
            if (uiState.savedImages.isEmpty()) {
                SzlEmptyState(
                    icon = Icons.Default.PhotoLibrary,
                    title = stringResource(R.string.profile_no_photos_title),
                    description = stringResource(R.string.profile_no_photos_desc),
                    actionLabel = stringResource(R.string.nav_studio),
                    onAction = { viewModel.navigateTo(AppDestination.STUDIO) }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMicro),
                    contentPadding = PaddingValues(bottom = AppDimens.ListBottomPadding),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                ) {
                    items(uiState.savedImages, key = { it.id }) { image ->
                        InstagramGridThumbnail(
                            image = image,
                            onClick = { viewModel.openImageDetail(image) }
                        )
                    }
                }
            }
        } else {
            // Tab 1: Thèmes & Presets (avec Photo de la Mire de Couleurs pour chaque preset)
            if (uiState.savedPresets.isEmpty()) {
                SzlEmptyState(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.profile_no_themes_title),
                    description = stringResource(R.string.profile_no_themes_desc),
                    actionLabel = stringResource(R.string.nav_palettes),
                    onAction = { viewModel.navigateTo(AppDestination.PALETTES) }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                    contentPadding = PaddingValues(bottom = AppDimens.ListBottomPadding),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.savedPresets, key = { it.id }) { preset ->
                        PresetCardWithColorSpectrum(
                            preset = preset,
                            onApply = { viewModel.applyPresetToStudio(preset) },
                            onExportCube = {
                                viewModel.exportCubeFromPreset(preset) { cubePath ->
                                    createCubeLauncher.launch(File(cubePath).name)
                                }
                            },
                            onDelete = { viewModel.deletePreset(preset.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Instagram-Style Square Grid Thumbnail (1:1 aspect ratio, downsampled loading).
 */
@Composable
private fun InstagramGridThumbnail(
    image: SavedGradedImage,
    onClick: () -> Unit
) {
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = File(image.filePath)
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    var sample = 1
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    while (maxDim / (sample * 2) >= 300) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = image.themeName ?: stringResource(R.string.profile_default_look_name),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.ProgressBarHeight),
                    strokeWidth = AppDimens.BorderThick,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Semantic gradient overlay at bottom with look label (Bug B10 fix)
        SzlBottomGradientScrim(
            text = image.themeName ?: stringResource(R.string.profile_default_look_name),
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

/**
 * Preset Card displaying the Visual Calibration Color Card (Mire) showing highlights,
 * grayscale ramp, midtones, and gamut reference colors.
 */
@Composable
private fun PresetCardWithColorSpectrum(
    preset: ColorimetryPreset,
    onApply: () -> Unit,
    onExportCube: () -> Unit,
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
                        while (maxDim / (sample * 2) >= 600) {
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
            // Header Row: Title & Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleMedium,
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
                        contentDescription = stringResource(R.string.profile_btn_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Visual Color Spectrum Card (Mire de calibration montrant les couleurs)
            Text(
                text = "Spectre & Étalonnage des Couleurs :",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.CalibrationCardLargeHeight)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .background(ColorCardReference.NeutralDarkBackground)
                    .border(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(AppDimens.CornerRadiusNormal)),
                contentAlignment = Alignment.Center
            ) {
                if (cardBitmap != null) {
                    Image(
                        bitmap = cardBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.palettes_mire_content_desc),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Fallback visual calibration card layout
                    FallbackVisualColorCard(paletteHexes = preset.paletteHexes)
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

            // Buttons: Apply to Studio & Export .CUBE
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
            ) {
                Button(
                    onClick = onApply,
                    modifier = Modifier
                        .weight(1.3f)
                        .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.profile_btn_apply_theme),
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onExportCube,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.profile_btn_export_cube),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Fallback procedural visual color card rendered with Compose if offline file is being generated.
 */
@Composable
private fun FallbackVisualColorCard(paletteHexes: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.SpacingNormal),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // Highlights
        Row(
            modifier = Modifier.fillMaxWidth().height(AppDimens.CalibrationRowHeightLarge),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusMire)).background(Color.White))
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusMire)).background(ColorCardReference.WhiteStep))
        }

        // Grayscale ramp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ProgressIndicatorSize)
                .clip(RoundedCornerShape(AppDimens.CornerRadiusMire))
                .background(
                    Brush.horizontalGradient(ColorCardReference.NeutralRamp)
                )
        )

        // Midtones
        Row(
            modifier = Modifier.fillMaxWidth().height(AppDimens.CalibrationRowHeightStandard),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusMire)).background(ColorCardReference.GrayMiddle1))
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusMire)).background(ColorCardReference.GrayMiddle2))
        }

        // 12 patches
        val defaultColors = ColorCardReference.ColorCheckerPatches
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
            for (i in 0 until 6) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(defaultColors[i]))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
            for (i in 6 until 12) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(AppDimens.CornerRadiusTiny)).background(defaultColors[i]))
            }
        }
    }
}
