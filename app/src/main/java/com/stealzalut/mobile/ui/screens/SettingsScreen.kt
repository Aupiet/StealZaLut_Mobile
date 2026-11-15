package com.stealzalut.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.ui.navigation.AppDestination
import com.stealzalut.mobile.ui.screens.settings.SettingsAppearanceSubScreen
import com.stealzalut.mobile.ui.screens.settings.SettingsEngineSubScreen
import com.stealzalut.mobile.ui.screens.settings.SettingsExportSubScreen
import com.stealzalut.mobile.ui.screens.settings.SettingsStorageSubScreen
import com.stealzalut.mobile.ui.screens.studio.SectionTitleBanner
import com.stealzalut.mobile.ui.theme.AppThemes
import com.stealzalut.mobile.ui.theme.icons
import com.stealzalut.mobile.ui.viewmodel.StealZaLutViewModel

enum class SettingsSubPage {
    MAIN,
    APPEARANCE,
    EXPORT,
    ENGINE,
    STORAGE
}

/**
 * Modular Settings & Preferences Hub.
 * Conforms to modern Android ergonomics & Szl Design System:
 * - Dedicated sub-screens for each settings domain with smooth back-navigation.
 * - Live graphical UI replication mockups for theme previews.
 * - Zero hardcoded dimensions or deprecated color tokens.
 */
@Composable
fun SettingsScreen(
    viewModel: StealZaLutViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
    var currentSubPage by remember { mutableStateOf(SettingsSubPage.MAIN) }
    var showMaintenanceScreen by remember { mutableStateOf(false) }

    // Intercept back navigation when on a dedicated sub-screen
    BackHandler(enabled = currentSubPage != SettingsSubPage.MAIN) {
        currentSubPage = SettingsSubPage.MAIN
    }

    AnimatedContent(
        targetState = currentSubPage,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        label = "SettingsSubPageTransition"
    ) { subPage ->
        when (subPage) {
            SettingsSubPage.MAIN -> {
                SettingsHubScreen(
                    settings = settings,
                    cacheSizeMb = uiState.cacheSizeMb,
                    currentLanguageTag = uiState.currentLanguageTag,
                    onNavigateToSubPage = { currentSubPage = it },
                    onNavigateToGuide = { viewModel.navigateTo(AppDestination.GUIDE) },
                    modifier = modifier
                )
            }
            SettingsSubPage.APPEARANCE -> {
                SettingsAppearanceSubScreen(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onNavigateBack = { currentSubPage = SettingsSubPage.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubPage.EXPORT -> {
                SettingsExportSubScreen(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onNavigateBack = { currentSubPage = SettingsSubPage.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubPage.ENGINE -> {
                SettingsEngineSubScreen(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onNavigateBack = { currentSubPage = SettingsSubPage.MAIN },
                    modifier = modifier
                )
            }
            SettingsSubPage.STORAGE -> {
                SettingsStorageSubScreen(
                    settings = settings,
                    currentLanguageTag = uiState.currentLanguageTag,
                    cacheSizeMb = uiState.cacheSizeMb,
                    onSetLanguage = { viewModel.setLanguage(it) },
                    onClearCache = { viewModel.clearCache() },
                    onOpenMaintenance = { showMaintenanceScreen = true },
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onNavigateBack = { currentSubPage = SettingsSubPage.MAIN },
                    modifier = modifier
                )
            }
        }
    }

    if (showMaintenanceScreen) {
        MaintenanceMonitorScreen(
            viewModel = viewModel,
            onDismiss = { showMaintenanceScreen = false }
        )
    }
}

/**
 * Main Settings Hub displaying top-level cards for all dedicated sub-menus.
 */
@Composable
private fun SettingsHubScreen(
    settings: AppSettings,
    cacheSizeMb: Double,
    currentLanguageTag: String?,
    onNavigateToSubPage: (SettingsSubPage) -> Unit,
    onNavigateToGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val currentThemeName = when (settings.selectedThemeId) {
        AppThemes.DEFAULT_THEME_ID -> "AmberNoir (Signature)"
        AppThemes.THEME_CYAN_STUDIO -> "CyanStudio (Pro Lab)"
        AppThemes.THEME_OLED_BLACK -> "OledBlack (Contraste Pur)"
        AppThemes.THEME_DAYLIGHT -> "Daylight (Clair Chaud)"
        else -> "Système par défaut"
    }

    val exportResolutionLabel = when (settings.maxExportDim) {
        1920 -> "1080p FHD"
        2560 -> "1440p QHD"
        3840 -> "4K UHD"
        else -> "Natif"
    }

    val currentLangLabel = when (currentLanguageTag ?: "fr") {
        "en" -> "English"
        "es" -> "Español"
        else -> "Français"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
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
                .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                .clickable(onClick = onNavigateToGuide),
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
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(AppDimens.FeatureCategoryBadgeSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = MaterialTheme.icons.actionHelp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                    }
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

        // --- Sub-menu Navigation Hub Items ---
        SettingsHubNavCard(
            icon = MaterialTheme.icons.actionPalette,
            title = stringResource(R.string.settings_theme_header),
            subtitle = "$currentThemeName • ${if (settings.selectedIconSetId == "outlined") "Outlined" else "Rounded"}",
            onClick = { onNavigateToSubPage(SettingsSubPage.APPEARANCE) }
        )

        SettingsHubNavCard(
            icon = MaterialTheme.icons.actionExport,
            title = "Résolution & Export",
            subtitle = "$exportResolutionLabel • ${settings.exportFormat} ${settings.exportQuality}%",
            onClick = { onNavigateToSubPage(SettingsSubPage.EXPORT) }
        )

        SettingsHubNavCard(
            icon = MaterialTheme.icons.toolCalibration,
            title = "Moteur & 3D LUT",
            subtitle = "${stringResource(settings.defaultMethod.displayNameRes)} • ${stringResource(settings.defaultLutSize.labelRes)}",
            onClick = { onNavigateToSubPage(SettingsSubPage.ENGINE) }
        )

        SettingsHubNavCard(
            icon = MaterialTheme.icons.statusLanguage,
            title = "Langue & Stockage",
            subtitle = "$currentLangLabel • Cache: ${String.format("%.1f", cacheSizeMb)} Mo",
            onClick = { onNavigateToSubPage(SettingsSubPage.STORAGE) }
        )

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}

/**
 * Clickable card navigating to a dedicated sub-screen.
 */
@Composable
private fun SettingsHubNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
        ),
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(AppDimens.IconJumbo)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconNormal)
                    )
                }
            }
            Spacer(modifier = Modifier.width(AppDimens.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(AppDimens.SpacingMicro))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Icon(
                imageVector = MaterialTheme.icons.actionChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }
    }
}
