package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.ColorCardReference

/**
 * High-end Chromatic AI Processing Screen (Studio Step 4).
 * Features:
 * - Fluid infinite chromatic aura rotation representing multi-dimensional LUT matrix synthesis.
 * - Stage indicator with localized description and acceleration badge.
 */
@Composable
fun StudioStep4Computing(
    stageIndex: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "computeTransition") // GUARD_ALLOW
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation" // GUARD_ALLOW
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse" // GUARD_ALLOW
    )

    val chromaticSpectrum = listOf(
        ColorCardReference.HslRed,
        ColorCardReference.HslYellow,
        ColorCardReference.HslGreen,
        ColorCardReference.HslAqua,
        ColorCardReference.HslBlue,
        ColorCardReference.HslMagenta,
        ColorCardReference.HslRed
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.SpacingMassive),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Chromatic Pulsing Aura
            Box(
                modifier = Modifier.size(AppDimens.ComputeLoaderSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(AppDimens.ComputeLoaderSize)) {
                    val radius = (size.width / 2f) * pulseScale
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = chromaticSpectrum,
                            center = center
                        ),
                        radius = radius,
                        center = center,
                        style = Stroke(width = AppDimens.BorderThick.toPx())
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier.size(AppDimens.AvatarSize),
                    strokeWidth = AppDimens.BorderMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingExtraLarge))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimens.IconSmall),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                Text(
                    text = stringResource(R.string.wizard_step_4_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

            val stageText = when (stageIndex) {
                1 -> stringResource(R.string.wizard_step_4_stage_1)
                2 -> stringResource(R.string.wizard_step_4_stage_2)
                else -> stringResource(R.string.wizard_step_4_stage_3)
            }

            Text(
                text = stageText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                border = androidx.compose.foundation.BorderStroke(
                    AppDimens.BorderThin,
                    MaterialTheme.colorScheme.tertiary.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.SpacingNormal, vertical = AppDimens.SpacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSmall),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                    Text(
                        text = stringResource(R.string.wizard_step_4_tech_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
