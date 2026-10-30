package com.stealzalut.mobile.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

/**
 * Accessible palette swatches view complying with WCAG 2.1 AA.
 * Each swatch announces localized TalkBack text and supports tap to copy to clipboard with haptic feedback.
 * Zero hardcoded strings and zero hardcoded dimensions.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            colors.forEachIndexed { index, hex ->
                val composeColor = parseHexColor(hex)
                val hexUpper = hex.uppercase()
                val total = colors.size
                val a11yDescription = stringResource(R.string.swatch_a11y_format, index + 1, total, hexUpper)

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_HIGH),
                    border = androidx.compose.foundation.BorderStroke(
                        AppDimens.BorderThin,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .semantics(mergeDescendants = true) {
                            contentDescription = a11yDescription
                        }
                        .clickable(
                            role = Role.Button,
                            onClickLabel = copyLabel
                        ) {
                            clipboardManager.setText(AnnotatedString(hexUpper))
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(AppDimens.SwatchSize)
                                .clip(CircleShape)
                                .background(composeColor)
                                .border(
                                    width = AppDimens.BorderMedium,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = AppConstants.OPACITY_BORDER_MEDIUM),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = hexUpper,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
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
            else -> Color.Gray
        }
    } catch (_: Exception) {
        Color.Gray
    }
}
