package com.stealzalut.mobile.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.icons
import kotlin.math.roundToInt

/**
 * Dedicated Resolution & Export Settings Sub-screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsExportSubScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val exportResolutions = listOf(
        1920 to "1080p FHD",
        2560 to "1440p QHD",
        3840 to "4K UHD",
        0 to stringResource(R.string.settings_max_export_native)
    )

    val proxyResolutions = listOf(
        1280 to "720p (Rapide)",
        1920 to "1080p (Équilibré)",
        2560 to "1440p (Précis)",
        0 to "Natif"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingLarge)
    ) {
        // --- Top Bar with Back Button ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(AppDimens.MinInteractiveTarget)
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.statusArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Column {
                Text(
                    text = "Résolution & Export",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Qualité de rendu, proxies et encodage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Card 1: Direct Image Export Resolution ---
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
                Text(
                    text = stringResource(R.string.settings_export_dim),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Plafond de dimension pour l'enregistrement dans la galerie",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                onUpdateSettings(settings.copy(maxExportDim = dim))
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
            }
        }

        // --- Card 2: Statistical Analysis Proxy Resolution ---
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
                Text(
                    text = stringResource(R.string.settings_proxy_dim),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Taille d'échantillonnage pour le calcul des distributions de couleurs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                onUpdateSettings(settings.copy(analysisProxyDim = dim))
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
            }
        }

        // --- Card 3: Format d'exportation & Qualité ---
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
                Text(
                    text = stringResource(R.string.settings_export_format),
                    style = MaterialTheme.typography.titleMedium,
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
                                onUpdateSettings(settings.copy(exportFormat = format))
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

                if (settings.exportFormat.equals("JPEG", ignoreCase = true)) {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                    SzlSlider(
                        title = stringResource(R.string.settings_export_quality, settings.exportQuality),
                        value = settings.exportQuality / 100f,
                        onValueChange = {
                            onUpdateSettings(settings.copy(exportQuality = (it * 100).roundToInt().coerceIn(10, 100)))
                        },
                        min = 0.1f,
                        max = 1.0f,
                        step = 0.05f,
                        defaultValue = 0.95f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
