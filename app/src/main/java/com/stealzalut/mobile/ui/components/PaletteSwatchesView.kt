package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * Cinema Color Swatches View (Pantone / Wes Anderson / ShotDeck style).
 * Features:
 * - Proportional cinematic ribbon representing the color distribution of the frame.
 * - Elegant tactile swatch cards with uppercase Hex code, RGB breakdown, frame dominance %,
 *   and one-tap copy to clipboard with haptic feedback.
 */
@Composable
fun PaletteSwatchesView(
    title: String,
    colors: List<String>,
    modifier: Modifier = Modifier
) {
    if (colors.isEmpty()) return

    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val copyLabel = stringResource(R.string.swatch_copy_label)
    val cinemaBadge = stringResource(R.string.palette_shot_cinema)
    val scrollState = rememberScrollState()

    // Harmonic proportions according to prominence rank
    val baseWeights = listOf(34, 24, 18, 12, 7, 5)
    val totalWeight = colors.indices.sumOf { baseWeights.getOrElse(it) { 5 } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- Header with Cinema Style Badge ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.primary.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MovieFilter,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconMicro),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingMicro))
                    Text(
                        text = cinemaBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // --- 1. Continuous Cinema Ribbon (Proportional Color Distribution) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.CinemaRibbonHeight)
                .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                .border(
                    width = AppDimens.BorderThin,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                    shape = RoundedCornerShape(AppDimens.CornerRadiusSmall)
                )
        ) {
            colors.forEachIndexed { index, hex ->
                val weight = baseWeights.getOrElse(index) { 5 }
                Box(
                    modifier = Modifier
                        .weight(weight.toFloat())
                        .height(AppDimens.CinemaRibbonHeight)
                        .background(parseHexColor(hex))
                )
            }
        }

        // --- 2. Horizontal Cinema Swatch Cards (Pantone / ShotDeck style) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
        ) {
            colors.forEachIndexed { index, hex ->
                val composeColor = parseHexColor(hex)
                val hexUpper = hex.uppercase()
                val weight = (baseWeights.getOrElse(index) { 5 } * 100) / totalWeight
                val dominanceText = stringResource(R.string.palette_dominance_format, weight)
                val a11yDesc = stringResource(R.string.swatch_a11y_format, index + 1, colors.size, hexUpper)

                Card(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM)
                    ),
                    modifier = Modifier
                        .width(AppDimens.CinemaSwatchCompactWidth)
                        .height(AppDimens.CinemaSwatchCompactHeight)
                        .semantics(mergeDescendants = true) {
                            contentDescription = a11yDesc
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = copyLabel
                        ) {
                            clipboardManager.setText(AnnotatedString(hexUpper))
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top Color Block
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.CinemaSwatchCompactColorHeight)
                                .background(composeColor),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = ColorCardReference.NeutralDarkBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(AppDimens.SpacingMicro)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = copyLabel,
                                    tint = ColorCardReference.WhiteStep,
                                    modifier = Modifier
                                        .size(AppDimens.IconMicro)
                                        .padding(AppDimens.SpacingMicro)
                                )
                            }
                        }

                        // Bottom Pantone Label Block (Stacked Hex and % with zero horizontal collision)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingMicro),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hexUpper,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = "$weight%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return try {
        when (clean.length) {
            6 -> {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r, g, b)
            }
            8 -> {
                val a = clean.substring(0, 2).toInt(16)
                val r = clean.substring(2, 4).toInt(16)
                val g = clean.substring(4, 6).toInt(16)
                val b = clean.substring(6, 8).toInt(16)
                Color(r, g, b, a)
            }
            else -> ColorCardReference.GrayMiddle2
        }
    } catch (_: Exception) {
        ColorCardReference.GrayMiddle2
    }
}
