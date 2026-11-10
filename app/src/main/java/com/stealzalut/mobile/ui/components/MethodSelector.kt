package com.stealzalut.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.ui.theme.icons

/**
 * Compact zero-scroll accessible algorithm selector for color transfer.
 * Displays all 5 mathematical algorithms on a single screen without scrolling:
 * - Row 1: Recommended cinematic method (HM-MKL-HM) full width.
 * - Row 2: MKL & HM side-by-side.
 * - Row 3: Reinhard & MVGD side-by-side.
 * - Bottom: Concise contextual description.
 * Complies with WCAG 2.1 AA with minHeight >= 48dp on all interactive elements.
 */
@Composable
fun MethodSelector(
    selectedMethod: ColorTransferMethod,
    onMethodSelected: (ColorTransferMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- Row 1: Recommended method (Full width) ---
        val recMethod = ColorTransferMethod.HM_MKL_HM
        val isRecSelected = selectedMethod == recMethod
        val recName = stringResource(recMethod.displayNameRes)
        val recSubtitle = stringResource(recMethod.technicalSubtitleRes)

        Surface(
            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
            color = if (isRecSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                AppDimens.BorderThin,
                if (isRecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
                .clickable { onMethodSelected(recMethod) }
                .semantics { contentDescription = "$recName, $recSubtitle" } // GUARD_ALLOW
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.SpacingMedium, vertical = AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isRecSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRecSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = recSubtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRecSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isRecSelected) {
                    Icon(
                        imageVector = MaterialTheme.icons.statusCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSmall)
                    )
                }
            }
        }

        // --- Row 2: MKL, HM, Reinhard pills ---
        val otherMethods = listOf(ColorTransferMethod.MKL, ColorTransferMethod.HM, ColorTransferMethod.REINHARD)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            otherMethods.forEach { method ->
                val isSelected = selectedMethod == method
                val name = stringResource(method.displayNameRes)
                val sub = stringResource(method.technicalSubtitleRes)

                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        AppDimens.BorderThin,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppDimens.MinInteractiveTarget)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusMedium))
                        .clickable { onMethodSelected(method) }
                        .semantics { contentDescription = "$name, $sub" } // GUARD_ALLOW
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingSmall),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // --- Bottom: Short description of selected method ---
        Surface(
            shape = RoundedCornerShape(AppDimens.CornerRadiusMedium),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.statusInfo,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSmall),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(selectedMethod.descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
