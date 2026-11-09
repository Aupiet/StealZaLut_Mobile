package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.theme.LocalFontSpec

/**
 * Step 4: Studio Computation Progress (Figma Node 12:329).
 * Strictly conforms to Figma [APRÈS] Studio / Étape 4 Calcul:
 * - Card with centered circular progress ring
 * - Centered percentage display in JetBrains Mono Bold (32sp)
 * - Stage title (e.g. "Interpolation Tetrahedral LUT 33x33x33")
 * - Stage subtitle (e.g. "Étape 3 sur 4 : Conservation des luminances")
 * - Fine amber linear progress bar
 */
@Composable
fun StudioStep4Computing(
    stageIndex: Int,
    modifier: Modifier = Modifier
) {
    val monoFont = LocalFontSpec.current.mono
    val progressPercent = when (stageIndex) {
        1 -> 25
        2 -> 50
        3 -> 74
        else -> 92
    }
    val progressFloat = progressPercent / 100f

    val infiniteTransition = rememberInfiniteTransition(label = "computeTransition") // GUARD_ALLOW
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = progressFloat - 0.05f,
        targetValue = progressFloat + 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressPulse" // GUARD_ALLOW
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
        ),
        border = BorderStroke(
            AppDimens.BorderThin,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.SpacingHuge, horizontal = AppDimens.SpacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large Circular Progress Ring with Center Percentage (Figma node 12:351)
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Track
                CircularProgressIndicator(
                    progress = { 1.0f },
                    modifier = Modifier.size(140.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                // Active Amber Arc
                CircularProgressIndicator(
                    progress = { animatedProgress.coerceIn(0.1f, 0.98f) },
                    modifier = Modifier.size(140.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                // Center Percentage in JetBrains Mono Bold (32sp)
                Text(
                    text = "$progressPercent %",
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = monoFont),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpacingLarge))

            // Stage Title
            Text(
                text = stringResource(R.string.wizard_step_4_title_stage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))

            // Stage Subtitle
            val stageSubtitle = when (stageIndex) {
                1 -> stringResource(R.string.wizard_step_4_stage_1)
                2 -> stringResource(R.string.wizard_step_4_stage_2)
                else -> stringResource(R.string.wizard_step_4_stage_3)
            }
            Text(
                text = stageSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

            // Horizontal Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress.coerceIn(0.1f, 0.98f) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(AppDimens.EditorSliderTrackHeight)
                    .clip(RoundedCornerShape(AppDimens.CornerRadiusPill)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
