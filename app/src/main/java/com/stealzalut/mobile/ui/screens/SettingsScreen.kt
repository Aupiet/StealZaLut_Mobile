package com.stealzalut.mobile.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.LutSize
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.AppThemes
import com.stealzalut.mobile.ui.theme.ThemeSpec
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
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSpacious)
    ) {
        // Quick Guide & Metrics Card Banner
        Card(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.openMetricsInfo()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.SpacingStandard),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.AvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    )
                }
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(
                        text = stringResource(R.string.settings_guide_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_guide_card_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section 1: Appearance & Visual Themes
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
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconNormal),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_section_appearance),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_theme_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Visual Theme Palette Cards Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    AppThemes.allThemes.forEach { themeSpec ->
                        val isSelected = settings.selectedThemeId == themeSpec.id
                        ThemePreviewCard(
                            theme = themeSpec,
                            isSelected = isSelected,
                            onSelect = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.updateSettings(settings.copy(selectedThemeId = themeSpec.id))
                            }
                        )
                    }
                }

                // Developer Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_developer_mode_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.settings_developer_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.developerMode,
                        onCheckedChange = { isChecked ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.updateSettings(settings.copy(developerMode = isChecked))
                        }
                    )
                }
            }
        }

        // Section 2: Colorimetric Engine & Default Parameters
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    SzlSlider(
                        title = stringResource(R.string.settings_quality_slider_desc),
                        value = settings.exportQuality / 100.0f,
                        onValueChange = { frac ->
                            viewModel.updateSettings(settings.copy(exportQuality = (frac * 100.0f).roundToInt().coerceIn(AppConstants.QUALITY_MIN.toInt(), AppConstants.QUALITY_MAX.toInt())))
                        },
                        min = AppConstants.QUALITY_MIN / 100.0f,
                        max = AppConstants.QUALITY_MAX / 100.0f,
                        step = 0.05f,
                        presets = listOf(0.70f, 0.85f, 0.95f, 1.0f)
                    )
                }
            }
        }

        // Section 4: Storage, Cache & In-App Language
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
                // Language Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
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
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setLanguage(tag)
                            },
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

                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

                // Cache Cleanup
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearCache()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
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
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}

@Composable
private fun ThemePreviewCard(
    theme: ThemeSpec,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val themeName = stringResource(theme.nameRes)
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        animationSpec = tween(durationMillis = 200),
        label = "themeBorder" // GUARD_ALLOW
    )

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
        color = theme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
            animatedBorderColor
        ),
        modifier = Modifier
            .defaultMinSize(minHeight = AppDimens.MinTouchTarget)
            .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
            .clickable(
                role = Role.RadioButton,
                onClick = onSelect
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Box(
                modifier = Modifier
                    .size(AppDimens.IconSmall)
                    .clip(CircleShape)
                    .background(theme.colorScheme.primary)
            )
            Text(
                text = themeName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = theme.colorScheme.onSurface
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = theme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconMicro)
                )
            }
        }
    }
}
