package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.icons

/**
 * Pinned Unified Bottom Navigation Bar for Studio (Prototype Mobile v2.1).
 * Strictly guarantees that "Retour" and "Continuer" / "Terminer" buttons are at the
 * exact same screen coordinates across all Studio steps (1, 2, 3, 5).
 * Button texts are short, concise, and never overflow button bounds.
 */
@Composable
fun StudioBottomBar(
    currentStep: Int,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingSmall),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: "Retour" (present on all steps)
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Icon(
                    imageVector = MaterialTheme.icons.statusArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.studio_btn_back),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Button: "Continuer" / "Rendu" / "Terminer" (Default / Primary)
            val rightButtonText = when (currentStep) {
                AppConstants.STEP_3_PARAMS -> stringResource(R.string.studio_btn_render)
                AppConstants.STEP_5_RESULT -> stringResource(R.string.studio_btn_finish)
                else -> stringResource(R.string.studio_btn_continue)
            }
            val rightButtonIcon = when (currentStep) {
                AppConstants.STEP_3_PARAMS -> MaterialTheme.icons.toolEffects
                AppConstants.STEP_5_RESULT -> MaterialTheme.icons.statusCheck
                else -> MaterialTheme.icons.statusArrowForward
            }

            Button(
                onClick = onNext,
                enabled = canProceed,
                modifier = Modifier
                    .weight(1.3f)
                    .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
                shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
            ) {
                Text(
                    text = rightButtonText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Icon(
                    imageVector = rightButtonIcon,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconNormal)
                )
            }
        }
    }
}
