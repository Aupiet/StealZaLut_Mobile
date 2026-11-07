package com.stealzalut.mobile.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens

/**
 * Concise, visual and playful metrics & algorithm guide.
 * Explains color transfer methods, 3D LUT lattice sizes, intensity/luminance dosage,
 * and palette extraction with zero wall-of-text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsInfoBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        MetricsInfoContent(
            onClose = onDismiss,
            modifier = Modifier.padding(bottom = AppDimens.SpacingMassive)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsInfoScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.info_screen_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.common_close)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        MetricsInfoContent(
            onClose = onClose,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}

@Composable
fun MetricsInfoContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = AppDimens.ScreenPaddingHorizontal, vertical = AppDimens.ScreenPaddingVertical),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSpacious)
    ) {
        // Subtitle Banner
        Text(
            text = stringResource(R.string.info_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ====================================================================
        // Section 1: Transfer Methods
        // ====================================================================
        InfoSectionHeader(
            title = stringResource(R.string.info_section_methods_title),
            icon = Icons.Default.Tune
        )

        CompactInfoCard(
            titleRes = R.string.info_method_hybrid_title,
            catchphraseRes = R.string.info_method_hybrid_catch,
            idealForRes = R.string.info_method_hybrid_ideal,
            icon = Icons.Default.AutoAwesome,
            isRecommended = true
        )

        CompactInfoCard(
            titleRes = R.string.info_method_reinhard_title,
            catchphraseRes = R.string.info_method_reinhard_catch,
            idealForRes = R.string.info_method_reinhard_ideal,
            icon = Icons.Default.BrightnessMedium
        )

        CompactInfoCard(
            titleRes = R.string.info_method_hm_title,
            catchphraseRes = R.string.info_method_hm_catch,
            idealForRes = R.string.info_method_hm_ideal,
            icon = Icons.Default.ColorLens
        )

        CompactInfoCard(
            titleRes = R.string.info_method_mkl_title,
            catchphraseRes = R.string.info_method_mkl_catch,
            idealForRes = R.string.info_method_mkl_ideal,
            icon = Icons.Default.MovieFilter
        )

        // ====================================================================
        // Section 2: 3D LUT Lattice Sizes
        // ====================================================================
        InfoSectionHeader(
            title = stringResource(R.string.info_section_lut_size_title),
            icon = Icons.Default.GridOn
        )

        CompactInfoCard(
            titleRes = R.string.info_lut_17_title,
            catchphraseRes = R.string.info_lut_17_catch,
            idealForRes = R.string.info_lut_17_ideal,
            icon = Icons.Default.ShutterSpeed
        )

        CompactInfoCard(
            titleRes = R.string.info_lut_33_title,
            catchphraseRes = R.string.info_lut_33_catch,
            idealForRes = R.string.info_lut_33_ideal,
            icon = Icons.Default.GridOn,
            isRecommended = true
        )

        CompactInfoCard(
            titleRes = R.string.info_lut_65_title,
            catchphraseRes = R.string.info_lut_65_catch,
            idealForRes = R.string.info_lut_65_ideal,
            icon = Icons.Default.AutoAwesome
        )

        // ====================================================================
        // Section 3: Color & Exposure Controls
        // ====================================================================
        InfoSectionHeader(
            title = stringResource(R.string.info_section_controls_title),
            icon = Icons.AutoMirrored.Filled.HelpOutline
        )

        CompactInfoCard(
            titleRes = R.string.info_control_intensity_title,
            catchphraseRes = R.string.info_control_intensity_catch,
            idealForRes = R.string.info_control_intensity_ideal,
            icon = Icons.Default.Tune
        )

        CompactInfoCard(
            titleRes = R.string.info_control_lum_title,
            catchphraseRes = R.string.info_control_lum_catch,
            idealForRes = R.string.info_control_lum_ideal,
            icon = Icons.Default.BrightnessMedium
        )

        // ====================================================================
        // Section 4: Palette & Color DNA
        // ====================================================================
        InfoSectionHeader(
            title = stringResource(R.string.info_section_palette_title),
            icon = Icons.Default.Palette
        )

        CompactInfoCard(
            titleRes = R.string.info_palette_title,
            catchphraseRes = R.string.info_palette_catch,
            idealForRes = R.string.info_palette_ideal,
            icon = Icons.Default.Palette
        )

        Spacer(modifier = Modifier.height(AppDimens.SpacingMedium))

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Text(
                text = stringResource(R.string.info_screen_btn_close),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoSectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppDimens.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(AppDimens.IconNormal)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CompactInfoCard(
    @StringRes titleRes: Int,
    @StringRes catchphraseRes: Int,
    @StringRes idealForRes: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CornerRadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecommended)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isRecommended)
            CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary))
        else
            CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.SpacingStandard),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppDimens.IconLarge)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(AppDimens.IconSmall),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerRadiusPill),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(
                                horizontal = AppDimens.SpacingSmall,
                                vertical = AppDimens.SpacingMicro
                            )
                        )
                    }
                }
            }

            Text(
                text = stringResource(catchphraseRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusMicro),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier.padding(top = AppDimens.SpacingMicro)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppDimens.SpacingMedium,
                        vertical = AppDimens.SpacingMicro
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                ) {
                    Text(
                        text = stringResource(R.string.info_ideal_for_prefix) + stringResource(idealForRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
