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
                context.startActivity(Intent.createChooser(intent, "Partager la photo"))
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
                EmptyStateCard(
                    title = stringResource(R.string.profile_no_photos_title),
                    description = stringResource(R.string.profile_no_photos_desc),
                    actionLabel = stringResource(R.string.nav_studio),
                    onAction = { viewModel.navigateTo(AppDestination.STUDIO) }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
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
                EmptyStateCard(
                    title = stringResource(R.string.profile_no_themes_title),
                    description = stringResource(R.string.profile_no_themes_desc),
                    actionLabel = stringResource(R.string.nav_palettes),
                    onAction = { viewModel.navigateTo(AppDestination.PALETTES) }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
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
                contentDescription = image.themeName ?: "Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Gradient overlay at bottom with theme label
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = image.themeName ?: "Look",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
                    .height(140.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .background(Color(0xFF202228))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(AppDimens.CornerRadiusNormal)),
                contentAlignment = Alignment.Center
            ) {
                if (cardBitmap != null) {
                    Image(
                        bitmap = cardBitmap!!.asImageBitmap(),
                        contentDescription = "Mire des couleurs du preset",
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
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Highlights
        Row(
            modifier = Modifier.fillMaxWidth().height(22.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(Color.White))
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(Color(0xFFF0F0F0)))
        }

        // Grayscale ramp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color(0xFF333333), Color(0xFF666666), Color(0xFF999999), Color(0xFFCCCCCC), Color.White)
                    )
                )
        )

        // Midtones
        Row(
            modifier = Modifier.fillMaxWidth().height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(Color(0xFF767676)))
            Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(Color(0xFF808080)))
        }

        // 12 patches
        val defaultColors = listOf(
            Color(0xFFCD9B84), Color(0xFF9B6E55), Color(0xFF5F3E2D),
            Color(0xFFE12D2D), Color(0xFF2DB937), Color(0xFF2D55E1),
            Color(0xFF2DD7E1), Color(0xFFD72DD7), Color(0xFFEBD723),
            Color(0xFF4B7DB9), Color(0xFF559B55), Color(0xFFD77D2D)
        )
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until 6) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(defaultColors[i]))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 6 until 12) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(2.dp)).background(defaultColors[i]))
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconExtraLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
            ) {
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
