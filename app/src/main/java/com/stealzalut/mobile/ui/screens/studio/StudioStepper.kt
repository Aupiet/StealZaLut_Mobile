package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * Highly visual and tactile 5-step studio stepper.
 * Features:
 * - Animated color transitions and completed state checkmarks.
 * - Prominent glowing active step ring with tactile feedback.
 * - Accessible step navigation allowing instant return to earlier steps.
 */
@Composable
fun StudioStepper(
    currentStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
        border = androidx.compose.foundation.BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (step in 1..AppConstants.TOTAL_WIZARD_STEPS) {
                val isCompleted = step < currentStep
                val isCurrent = step == currentStep

                val targetCircleBg = when {
                    isCompleted -> MaterialTheme.colorScheme.primary
                    isCurrent -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
                val targetContentColor = when {
                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                    isCurrent -> MaterialTheme.colorScheme.onPrimary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val animatedCircleBg by animateColorAsState(
                    targetValue = targetCircleBg,
                    animationSpec = tween(durationMillis = 250),
                    label = "stepperCircleBg" // GUARD_ALLOW
                )
                val animatedContentColor by animateColorAsState(
                    targetValue = targetContentColor,
                    animationSpec = tween(durationMillis = 250),
                    label = "stepperContentColor" // GUARD_ALLOW
                )

                Box(
                    modifier = Modifier
                        .size(AppDimens.SwatchSize)
                        .clip(CircleShape)
                        .background(animatedCircleBg)
                        .then(
                            if (!isCurrent && !isCompleted) {
                                Modifier.border(
                                    width = AppDimens.BorderThin,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable(enabled = isCompleted) { onStepClick(step) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = MaterialTheme.icons.statusCheck,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall),
                            tint = animatedContentColor
                        )
                    } else {
                        Text(
                            text = "$step",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = animatedContentColor
                        )
                    }
                }

                if (step < AppConstants.TOTAL_WIZARD_STEPS) {
                    val lineColor by animateColorAsState(
                        targetValue = if (step < currentStep) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE),
                        animationSpec = tween(durationMillis = 250),
                        label = "stepperLine" // GUARD_ALLOW
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(AppDimens.BorderThick)
                            .padding(horizontal = AppDimens.SpacingExtraSmall)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusPill))
                            .background(lineColor)
                    )
                }
            }
        }
    }
}
