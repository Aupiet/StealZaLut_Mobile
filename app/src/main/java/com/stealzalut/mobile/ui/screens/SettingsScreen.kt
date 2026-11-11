package com.stealzalut.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.diagnostics.AppLog
import com.stealzalut.mobile.ui.navigation.AppDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner
import com.stealzalut.mobile.ui.theme.AppThemes
import com.stealzalut.mobile.ui.theme.LocalFontSpec
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlin.math.roundToInt

/**
 * Comprehensive Settings & Preferences Screen.
 * Conforms to Figma [APRÈS] Paramètres / Thèmes & Système:
 * - Playfair Display Header: "Préférences & Thèmes"
 * - Card 1: App Theme & Icon Set
 * - Card 2: Resolution & Export Settings (Export resolution, proxy resolution, format, JPEG quality)
 * - Card 3: Color Engine & 3D LUT (Default method, default lattice size)
 * - Card 4: Language, Cache Storage & Developer Mode
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val monoFont = LocalFontSpec.current.mono
    val diagnosticEntries = remember(settings.developerMode) {
        if (settings.developerMode) AppLog.current()?.recentEntries(8).orEmpty().asReversed()
        else emptyList()
    }

    val themesList = listOf(
        AppThemes.THEME_SYSTEM_DEFAULT to "Système par défaut (Auto Jour/Nuit)",
        AppThemes.DEFAULT_THEME_ID to "AmberNoir (Signature)",
        AppThemes.THEME_CYAN_STUDIO to "CyanStudio (Pro Lab)",
        AppThemes.THEME_OLED_BLACK to "OledBlack (Contraste Pur)",
        AppThemes.THEME_DAYLIGHT to "Daylight (Clair Chaud)"
    )

    val iconSetsList = listOf(
        "rounded" to "Rounded Icons (Chic & Doux)",
        "outlined" to "Outlined Icons (Précision Studio)"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)
    ) {
        // En-tête Playfair Display
        SectionTitleBanner(
            title = stringResource(R.string.settings_title),
            subtitle = stringResource(R.string.settings_subtitle)
        )

        // --- Card 0: Feature Guide Discovery ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(AppDestination.GUIDE) },
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
            ),
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingLarge),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.FeatureCategoryBadgeSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MaterialTheme.icons.actionHelp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_guide_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                    Text(
                        text = stringResource(R.string.settings_guide_card_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = MaterialTheme.icons.actionChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
        }

        // --- Card 1: Thème d'application & Jeu d'icônes ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
            ),
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                themesList.forEach { (themeId, label) ->
                    val isSelected = settings.selectedThemeId == themeId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(selectedThemeId = themeId))
                            },
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        border = BorderStroke(
                            if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                if (settings.developerMode) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = AppDimens.SpacingSmall),
                                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_developer_diagnostics_title),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (diagnosticEntries.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.settings_developer_diagnostics_empty),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            diagnosticEntries.forEach { entry ->
                                                Text(
                                                    text = stringResource(
                                                        R.string.settings_developer_diagnostics_entry,
                                                        entry.level.name,
                                                        entry.message
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

                // Jeu d'icônes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Text(
                        text = stringResource(R.string.settings_icons_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                iconSetsList.forEach { (iconSetId, label) ->
                    val isSelected = settings.selectedIconSetId == iconSetId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(selectedIconSetId = iconSetId))
                            },
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        border = BorderStroke(
                            if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Card 2: Résolution & Paramètres d'Exportation ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
            ),
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Text(
                        text = "Résolution & Paramètres d'Export",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 1. Direct Image Export Resolution
                Text(
                    text = stringResource(R.string.settings_export_dim),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val exportResolutions = listOf(
                    1920 to "1080p FHD",
                    2560 to "1440p QHD",
                    3840 to "4K UHD",
                    0 to stringResource(R.string.settings_max_export_native)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                ) {
                    exportResolutions.forEach { (dim, label) ->
                        val isSelected = settings.maxExportDim == dim
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(maxExportDim = dim))
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // 2. Statistical Analysis Proxy Resolution
                Text(
                    text = stringResource(R.string.settings_proxy_dim),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val proxyResolutions = listOf(
                    1280 to "720p (Rapide)",
                    1920 to "1080p (Équilibré)",
                    2560 to "1440p (Précis)",
                    0 to "Natif"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                ) {
                    proxyResolutions.forEach { (dim, label) ->
                        val isSelected = settings.analysisProxyDim == dim
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(analysisProxyDim = dim))
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // 3. Export Format (JPEG vs PNG)
                Text(
                    text = stringResource(R.string.settings_export_format),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    listOf("JPEG", "PNG").forEach { format ->
                        val isSelected = settings.exportFormat.equals(format, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(exportFormat = format))
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = format,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // 4. JPEG Quality Slider
                if (settings.exportFormat.equals("JPEG", ignoreCase = true)) {
                    SzlSlider(
                        title = stringResource(R.string.settings_export_quality, settings.exportQuality),
                        value = settings.exportQuality / 100f,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(exportQuality = (it * 100).roundToInt().coerceIn(10, 100)))
                        },
                        min = 0.1f,
                        max = 1.0f,
                        step = 0.05f,
                        defaultValue = 0.95f
                    )
                }
            }
        }

        // --- Card 3: Paramètres Moteur & Algorithme 3D LUT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
            ),
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Text(
                        text = "Moteur & Algorithme 3D LUT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Default Algorithm
                Text(
                    text = stringResource(R.string.settings_default_method),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    ColorTransferMethod.entries.forEach { method ->
                        val isSelected = settings.defaultMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(defaultMethod = method))
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = stringResource(method.displayNameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Default LUT Lattice Size
                Text(
                    text = stringResource(R.string.settings_default_lut_size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    LutSize.entries.forEach { size ->
                        val isSelected = settings.defaultLutSize == size
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(defaultLutSize = size))
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = stringResource(size.labelRes),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }

        // --- Card 4: Langue, Stockage Cache & Mode Développeur ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
            ),
            border = BorderStroke(
                AppDimens.BorderThin,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingLarge),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Text(
                        text = "Langue & Stockage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // In-App Language
                Text(
                    text = stringResource(R.string.settings_language_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val languages = listOf(
                    "fr" to stringResource(R.string.settings_lang_fr),
                    "en" to stringResource(R.string.settings_lang_en),
                    "es" to stringResource(R.string.settings_lang_es)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    languages.forEach { (code, label) ->
                        val isSelected = (uiState.currentLanguageTag ?: "fr") == code
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setLanguage(code)
                            },
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // Cache Cleanup
                Text(
                    text = stringResource(R.string.settings_cache_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.settings_cache_size, uiState.cacheSizeMb),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = monoFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearCache()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                ) {
                    Icon(
                        imageVector = Icons.Default.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.settings_btn_clear_cache),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Developer Mode Toggle
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_developer_mode_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.settings_developer_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.developerMode,
                            onCheckedChange = { checked ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(developerMode = checked))
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
