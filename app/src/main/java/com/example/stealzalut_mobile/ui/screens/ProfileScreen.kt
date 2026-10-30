package com.example.stealzalut_mobile.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.stealzalut_mobile.data.model.ColorimetryPreset
import com.example.stealzalut_mobile.data.model.SavedGradedImage
import com.example.stealzalut_mobile.ui.components.ColorSetDetailDialog
import com.example.stealzalut_mobile.ui.components.PaletteSwatchesView
import com.example.stealzalut_mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context: Context = LocalContext.current
    var selectedFolderTab by remember { mutableIntStateOf(0) } // 0 = Thèmes, 1 = Images, 2 = Paramètres

    // SAF Document Creator launcher for exporting a theme's mire
    val exportMireLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destinationUri ->
        if (destinationUri != null && uiState.selectedMirePreset != null) {
            viewModel.saveMireToUri(uiState.selectedMirePreset!!, destinationUri)
        }
    }

    // Detail Dialog for inspecting and exporting the Mire of a theme
    if (uiState.selectedMirePreset != null) {
        ColorSetDetailDialog(
            preset = uiState.selectedMirePreset!!,
            colorSetBitmap = uiState.selectedMireBitmap,
            onDismiss = { viewModel.closeMireDetail() },
            onExportMire = { preset ->
                val fileName = "StealZaLUT_Mire_${preset.name.replace(" ", "_")}.jpg"
                exportMireLauncher.launch(fileName)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("📁", style = MaterialTheme.typography.headlineMedium)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mon Profil & Dossiers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.savedThemes.size} Thèmes sauvegardés • ${uiState.savedImages.size} Photos étalonnées",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Folder Tabs: Thèmes / Images / Paramètres
        TabRow(
            selectedTabIndex = selectedFolderTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(10.dp))
        ) {
            Tab(
                selected = selectedFolderTab == 0,
                onClick = { selectedFolderTab = 0 },
                text = { Text("🎨 Thèmes (${uiState.savedThemes.size})", style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = selectedFolderTab == 1,
                onClick = { selectedFolderTab = 1 },
                text = { Text("🖼️ Images (${uiState.savedImages.size})", style = MaterialTheme.typography.labelSmall) }
            )
            Tab(
                selected = selectedFolderTab == 2,
                onClick = { selectedFolderTab = 2 },
                text = { Text("⚙️ Paramètres", style = MaterialTheme.typography.labelSmall) }
            )
        }

        // Tab Content
        when (selectedFolderTab) {
            // -------------------------------------------------------------
            // Folder 1 : Mes Thèmes Sauvegardés (avec aperçu & export de mire)
            // -------------------------------------------------------------
            0 -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.savedThemes, key = { it.id }) { theme ->
                        SavedThemeItemCard(
                            theme = theme,
                            onMireClick = { viewModel.openMireDetail(theme) },
                            onApply = { viewModel.applyThemeFromLibrary(theme) },
                            onDelete = { viewModel.deleteTheme(theme.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // -------------------------------------------------------------
            // Folder 2 : Mes Images Étalonnées (Dossier exports)
            // -------------------------------------------------------------
            1 -> {
                if (uiState.savedImages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🖼️", style = MaterialTheme.typography.displayMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucune photo étalonnée pour le moment",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Étalonnez une photo dans l'onglet 'Appliquer' pour la retrouver ici.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.savedImages, key = { it.id }) { img ->
                            SavedImageItemCard(
                                image = img,
                                onShare = {
                                    viewModel.getShareIntent(img.filePath)?.let { intent ->
                                        context.startActivity(android.content.Intent.createChooser(intent, "Partager l'image"))
                                    }
                                },
                                onDelete = { viewModel.deleteSavedImage(img.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }

            // -------------------------------------------------------------
            // Folder 3 : Paramètres Système & Cache
            // -------------------------------------------------------------
            2 -> {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

/**
 * Theme Card with integrated preview of the LUT Color Set (Mire).
 * Clicking on the mire opens the detail dialog allowing high-res inspection and export!
 */
@Composable
private fun SavedThemeItemCard(
    theme: ColorimetryPreset,
    onMireClick: () -> Unit,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(theme.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (theme.isBuiltIn) "Look Officiel" else "Créé le $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (theme.isBuiltIn) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "OFFICIEL",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Text("🗑️", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = theme.method.displayName,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${(theme.intensity * 100).roundToInt()}% Opacité",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = theme.lutSize.label,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // =================================================================
            // PREVIEW DE LA MIRE COLOR SET (Cliquable pour agrandir & exporter)
            // =================================================================
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMireClick() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mire Thumbnail
                    Box(
                        modifier = Modifier
                            .size(70.dp, 48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val mireFile = theme.colorSetImagePath?.let { File(it) }
                        if (mireFile != null && mireFile.exists()) {
                            val bmp = remember(theme.colorSetImagePath) {
                                BitmapFactory.decodeFile(mireFile.absolutePath)
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Mire",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("🎯", style = MaterialTheme.typography.titleMedium)
                            }
                        } else {
                            Text("🎯", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mire de Calibration du Look",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Touchez pour inspecter & exporter la mire",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "Voir ↗",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Palette Swatches
            if (theme.paletteHexes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                PaletteSwatchesView(
                    title = "Nuancier dominant :",
                    colors = theme.paletteHexes
                )
            }

            // Apply Button
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "✨ Appliquer ce Thème à une Photo",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Card representing an exported graded image in the user's profile folder.
 */
@Composable
private fun SavedImageItemCard(
    image: SavedGradedImage,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(image.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp, 80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val f = File(image.filePath)
                if (f.exists()) {
                    val bmp = remember(image.filePath) {
                        BitmapFactory.decodeFile(f.absolutePath)
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Photo étalonnée",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = image.themeName ?: "Look StealZaLUT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (image.width > 0) "${image.width}×${image.height} px" else "Haute Résolution",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Share & Delete
            Row {
                IconButton(onClick = onShare) {
                    Text("📤")
                }
                IconButton(onClick = onDelete) {
                    Text("🗑️")
                }
            }
        }
    }
}
