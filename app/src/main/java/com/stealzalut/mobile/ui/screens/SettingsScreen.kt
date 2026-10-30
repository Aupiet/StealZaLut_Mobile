package com.stealzalut.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSpacious)
    ) {
        // Section 0: In-App Language Selector (Placed at top for instant visibility)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    val currentLang = uiState.currentLanguageTag.lowercase()
                    listOf(
                        AppConstants.LANG_TAG_FR to stringResource(R.string.settings_lang_fr),
                        AppConstants.LANG_TAG_EN to stringResource(R.string.settings_lang_en),
                        AppConstants.LANG_TAG_ES to stringResource(R.string.settings_lang_es)
                    ).forEach { (tag, label) ->
                        val isSelected = currentLang.startsWith(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLanguage(tag) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = AppDimens.MinTouchTarget)
                                .semantics { contentDescription = label },
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

        // Section 1: Default Grading Settings
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.settings_default_lut_size),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // FlowRow prevents vertical squishing of 65³ chip on small screens
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    LutSize.entries.forEach { size ->
                        val isSelected = settings.defaultLutSize == size
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(settings.copy(defaultLutSize = size))
                            },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = stringResource(size.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

                Text(
                    text = stringResource(R.string.settings_default_method),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)) {
                    ColorTransferMethod.entries.forEach { method ->
                        val isSelected = settings.defaultMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(settings.copy(defaultMethod = method))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Column(modifier = Modifier.padding(vertical = AppDimens.SpacingExtraSmall)) {
                                    Text(
                                        text = stringResource(method.displayNameRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = stringResource(method.technicalSubtitleRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = AppConstants.OPACITY_CONTAINER_HEAVY)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Storage & Cache Management
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier.padding(AppDimens.SpacingStandard),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Text(
                    text = stringResource(R.string.settings_cache_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_cache_size, uiState.cacheSizeMb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { viewModel.clearCache() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                ) {
                    Text(
                        text = stringResource(R.string.settings_btn_clear_cache),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Section 3: Direct Image Export Settings
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.settings_export_dim),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // FlowRow prevents vertical squishing of Native chip
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    listOf(
                        AppConstants.EXPORT_DIM_1080P to stringResource(R.string.export_dim_1080p),
                        AppConstants.EXPORT_DIM_2K to stringResource(R.string.export_dim_2k),
                        AppConstants.EXPORT_DIM_4K to stringResource(R.string.export_dim_4k),
                        AppConstants.EXPORT_DIM_NATIVE to stringResource(R.string.export_dim_native)
                    ).forEach { (dim, label) ->
                        val isSelected = settings.maxExportDim == dim
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(settings.copy(maxExportDim = dim))
                            },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinTouchTarget),
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

                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

                Text(
                    text = stringResource(R.string.settings_export_format),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    listOf(AppConstants.EXPORT_FORMAT_JPEG, AppConstants.EXPORT_FORMAT_PNG).forEach { fmt ->
                        val isSelected = settings.exportFormat == fmt
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                viewModel.updateSettings(settings.copy(exportFormat = fmt))
                            },
                            modifier = Modifier.defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            label = {
                                Text(
                                    text = fmt,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                if (settings.exportFormat == AppConstants.EXPORT_FORMAT_JPEG) {
                    Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_export_quality, settings.exportQuality),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val sliderDesc = stringResource(R.string.settings_quality_slider_desc)
                    Slider(
                        value = settings.exportQuality.toFloat(),
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(exportQuality = it.roundToInt()))
                        },
                        valueRange = AppConstants.QUALITY_MIN..AppConstants.QUALITY_MAX,
                        steps = AppConstants.QUALITY_STEPS,
                        modifier = Modifier.semantics {
                            contentDescription = sliderDesc
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.SpacingSpacious))
    }
}
