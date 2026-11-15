package com.stealzalut.mobile.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.AppSettings
import com.stealzalut.mobile.ui.theme.AppThemes
import com.stealzalut.mobile.ui.theme.ThemeSpec
import com.stealzalut.mobile.ui.theme.icons

/**
 * Dedicated Appearance & Themes Settings Sub-screen.
 * Features:
 * - Real-time graphical UI replication mockup for each theme.
 * - Icon set selection (Rounded vs Outlined).
 * - Fully conforms to Szl Design System.
 */
@Composable
fun SettingsAppearanceSubScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val themeItems = listOf(
        Triple(
            AppThemes.DEFAULT_THEME_ID,
            AppThemes.AmberNoir,
            "AmberNoir (Signature)" to "Signature cinéma • Accent ambre & noirs profonds"
        ),
        Triple(
            AppThemes.THEME_CYAN_STUDIO,
            AppThemes.CyanStudio,
            "CyanStudio (Pro Lab)" to "Pro Lab • Accent cyan & tons froids étalonnés"
        ),
        Triple(
            AppThemes.THEME_OLED_BLACK,
            AppThemes.OledBlack,
            "OledBlack (Contraste Pur)" to "Contraste absolu • Fond noir pur 0% OLED & ambre"
        ),
        Triple(
            AppThemes.THEME_DAYLIGHT,
            AppThemes.Daylight,
            "Daylight (Clair Chaud)" to "Chambre claire • Interface lumineuse & tons chauds"
        ),
        Triple(
            AppThemes.THEME_SYSTEM_DEFAULT,
            AppThemes.getThemeSpec(AppThemes.THEME_SYSTEM_DEFAULT),
            "Système par défaut" to "Automatique • S'adapte au mode sombre / clair du système"
        )
    )

    val iconSets = listOf(
        "rounded" to ("Rounded Icons" to "Style moderne, doux et chic"),
        "outlined" to ("Outlined Icons" to "Précision studio & lignes nettes")
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
                    text = stringResource(R.string.settings_theme_header),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Aperçus graphiques & personnalisation visuelle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Section 1: Themes with Graphical Replication Mockups ---
        Text(
            text = "Thèmes Graphiques",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        themeItems.forEach { (themeId, spec, labels) ->
            val isSelected = settings.selectedThemeId == themeId
            ThemeGraphicalCard(
                themeSpec = spec,
                title = labels.first,
                subtitle = labels.second,
                isSelected = isSelected,
                onSelect = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUpdateSettings(settings.copy(selectedThemeId = themeId))
                }
            )
        }

        // --- Section 2: Icon Sets ---
        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))
        Text(
            text = stringResource(R.string.settings_icons_header),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        iconSets.forEach { (iconSetId, labels) ->
            val isSelected = settings.selectedIconSetId == iconSetId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUpdateSettings(settings.copy(selectedIconSetId = iconSetId))
                    },
                shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                border = BorderStroke(
                    if (isSelected) AppDimens.BorderMedium else AppDimens.BorderThin,
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = labels.first,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = labels.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconMedium)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MaterialTheme.icons.statusCheck,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}

/**
 * Graphical card reproducing a live miniature mockup of the selected theme.
 */
@Composable
private fun ThemeGraphicalCard(
    themeSpec: ThemeSpec,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
        ),
        border = BorderStroke(
            if (isSelected) AppDimens.BorderThick else AppDimens.BorderThin,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
        ) {
            // Live Miniature Mockup Frame
            ThemeMiniatureMockup(themeSpec = themeSpec)

            // Label Row with Radio Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                if (isSelected) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconMedium)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = MaterialTheme.icons.statusCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(AppDimens.IconSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Miniature smartphone UI representation in the exact colors and contrast of the target ThemeSpec.
 */
@Composable
private fun ThemeMiniatureMockup(
    themeSpec: ThemeSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(AppDimens.CinemaSwatchCompactHeight)
            .clip(RoundedCornerShape(AppDimens.CornerRadiusNormal)),
        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
        color = themeSpec.colorScheme.background,
        border = BorderStroke(
            AppDimens.BorderThin,
            themeSpec.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.SpacingSmall),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mini TopBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.SpacingNormal)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                    .background(themeSpec.colorScheme.surface)
                    .padding(horizontal = AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.SpacingSmall)
                        .clip(CircleShape)
                        .background(themeSpec.colorScheme.primary)
                )
                Box(
                    modifier = Modifier
                        .width(AppDimens.DrawerHandleWidth)
                        .height(AppDimens.BorderThick)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .background(themeSpec.colorScheme.onSurface.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(AppDimens.SpacingSmall)
                        .clip(CircleShape)
                        .background(themeSpec.colorScheme.outlineVariant)
                )
            }

            // Mini Viewport (Avant / Après Comparison with Split line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = AppDimens.SpacingMicro)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                    .background(themeSpec.szlColors.photoDarkroom),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Neutral raw
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(themeSpec.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(AppDimens.SpacingLarge)
                            .height(AppDimens.BorderMedium)
                            .background(themeSpec.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
                // Center split line
                Box(
                    modifier = Modifier
                        .width(AppDimens.BorderThin)
                        .fillMaxHeight()
                        .background(themeSpec.colorScheme.primary)
                )
                // Right side: Graded with theme primary glow
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(themeSpec.colorScheme.primary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(AppDimens.SpacingLarge)
                            .height(AppDimens.BorderMedium)
                            .background(themeSpec.colorScheme.primary)
                    )
                }
            }

            // Mini Bottom Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.SpacingMedium),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimens.SpacingNormal)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .background(themeSpec.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .weight(1.4f)
                        .height(AppDimens.SpacingNormal)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                        .background(themeSpec.colorScheme.primary)
                )
            }
        }
    }
}
