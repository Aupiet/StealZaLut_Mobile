package com.example.stealzalut_mobile.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.stealzalut_mobile.data.model.ColorimetryPreset
import com.example.stealzalut_mobile.ui.components.DoubleViewComparison
import com.example.stealzalut_mobile.ui.components.ImagePickerCard
import com.example.stealzalut_mobile.ui.components.IntensitySlider
import com.example.stealzalut_mobile.ui.components.LutSizeSelector
import com.example.stealzalut_mobile.ui.components.MethodSelector
import com.example.stealzalut_mobile.ui.components.PaletteSwatchesView
import com.example.stealzalut_mobile.ui.viewmodel.StealZaLutViewModel
import java.io.File

@Composable
fun ApplyThemeScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context: Context = LocalContext.current
    val scrollState = rememberScrollState()

    // SAF Document Creator launcher for exporting .cube file
    val createCubeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { destinationUri ->
        if (destinationUri != null) {
            viewModel.saveExportedCubeToUri(destinationUri)
        }
    }

    // SAF Document Creator launcher for exporting graded image
    val mimeType = if (uiState.settings.exportFormat.equals("PNG", ignoreCase = true)) "image/png" else "image/jpeg"
    val createImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { destinationUri ->
        if (destinationUri != null) {
            viewModel.saveExportedImageToUri(destinationUri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "✨ Appliquer un Thème sur votre Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choisissez votre photo source, sélectionnez un thème de votre profil ou un look cinéma, puis observez le rendu en Double Vue en direct.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Source Image Selection
        ImagePickerCard(
            title = "Photo Source à Étalonner",
            subtitle = "Touchez pour sélectionner votre cliché dans la galerie",
            badgeText = "À Étalonner",
            bitmap = uiState.sourceBitmap,
            onImageSelected = { viewModel.setSourceImage(it) }
        )

        // Theme Selector (Carousel of saved themes & built-in presets)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Sélectionnez le Thème Colorimétrique :",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.savedThemes, key = { it.id }) { theme ->
                    val isSelected = uiState.selectedTheme?.id == theme.id
                    Card(
                        onClick = { viewModel.selectThemeToApply(theme) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isSelected)
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        else
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = theme.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (theme.paletteHexes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    theme.paletteHexes.take(4).forEach { hex ->
                                        Surface(
                                            modifier = Modifier.height(6.dp).weight(1f),
                                            shape = RoundedCornerShape(2.dp),
                                            color = com.example.stealzalut_mobile.ui.components.parseHexColor(hex)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Theme Palette
        uiState.selectedTheme?.paletteHexes?.let { pal ->
            if (pal.isNotEmpty()) {
                PaletteSwatchesView(
                    title = "Palette du Thème '${uiState.selectedTheme?.name}' :",
                    colors = pal
                )
            }
        }

        // ====================================================================
        // VISUALISATION : DOUBLE VUE SEULEMENT (Conformément à la consigne)
        // ====================================================================
        DoubleViewComparison(
            sourceBitmap = uiState.sourceBitmap,
            previewBitmap = uiState.previewBitmap,
            executionTimeMs = uiState.previewExecutionTimeMs
        )

        // Grading Controls
        MethodSelector(
            selectedMethod = uiState.method,
            onMethodSelected = { viewModel.setMethod(it) }
        )

        IntensitySlider(
            intensity = uiState.intensity,
            onIntensityChange = { viewModel.setIntensity(it) }
        )

        LutSizeSelector(
            selectedSize = uiState.lutSize,
            onSizeSelected = { viewModel.setLutSize(it) }
        )

        // Export Actions Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Direct Graded Image Export
                Button(
                    onClick = {
                        viewModel.exportGradedImage { imagePath ->
                            createImageLauncher.launch(File(imagePath).name)
                        }
                    },
                    enabled = uiState.canApply,
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (uiState.isExportingImage) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Text("🎨 Exporter Image", fontWeight = FontWeight.Bold)
                    }
                }

                // Export .CUBE
                Button(
                    onClick = {
                        viewModel.exportCube { cubePath ->
                            createCubeLauncher.launch(File(cubePath).name)
                        }
                    },
                    enabled = uiState.canApply,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isExportingCube) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("💾 LUT .CUBE", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.exportedCubePath != null || uiState.exportedImagePath != null) {
                OutlinedButton(
                    onClick = {
                        val path = uiState.exportedCubePath ?: uiState.exportedImagePath
                        viewModel.getShareIntent(path)?.let { intent ->
                            context.startActivity(android.content.Intent.createChooser(intent, "Partager le rendu"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📤 Partager le rendu étalonné", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
