package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.szlColors

/**
 * Clean scrim and image label components.
 *
 * Resolves Bug B4 & Bug B10: Uses semantic `compareBadge` & `overlayOnImage` roles
 * with high readability and zero raw color literals.
 */
@Composable
fun BoxScope.SzlImageBadge(
    text: String,
    alignment: Alignment,
    modifier: Modifier = Modifier,
    isCompareMode: Boolean = false
) {
    val containerColor = if (isCompareMode) {
        MaterialTheme.szlColors.compareBadge
    } else {
        MaterialTheme.szlColors.overlayOnImage
    }

    val contentColor = if (isCompareMode) {
        MaterialTheme.szlColors.onCompareBadge
    } else {
        MaterialTheme.szlColors.onOverlayOnImage
    }

    Surface(
        modifier = modifier
            .align(alignment)
            .padding(AppDimens.SpacingSmall),
        shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
        color = containerColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingMicro),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SzlBottomGradientScrim(
    text: String,
    modifier: Modifier = Modifier
) {
    val scrimColor = MaterialTheme.szlColors.scrim
    val textColor = MaterialTheme.szlColors.onOverlayOnImage
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, scrimColor)
                )
            )
            .padding(horizontal = AppDimens.SpacingExtraSmall, vertical = AppDimens.SpacingMicro)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
