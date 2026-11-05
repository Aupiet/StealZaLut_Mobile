package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.SzlSlider

@Composable
fun EditorCalibrationTab(
    hasLut: Boolean,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    lumaLock: Float,
    onLumaLockChange: (Float) -> Unit,
    neutralLock: Float,
    onNeutralLockChange: (Float) -> Unit,
    skinToneLock: Float,
    onSkinToneLockChange: (Float) -> Unit,
    extremesRolloff: Float,
    onExtremesRolloffChange: (Float) -> Unit,
    spatial3d: Float,
    onSpatial3dChange: (Float) -> Unit,
    onResetTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingNormal)
    ) {
        if (!hasLut) {
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerRadiusSmall),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppDimens.SpacingMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(AppDimens.SpacingNormal))
                    Text(
                        text = stringResource(R.string.editor_calibration_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        SzlSlider(
            title = stringResource(R.string.editor_filter_intensity_label),
            value = intensity,
            onValueChange = onIntensityChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_luma_lock_title),
            value = lumaLock,
            onValueChange = onLumaLockChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_neutral_axis_lock_title),
            value = neutralLock,
            onValueChange = onNeutralLockChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_skin_tone_lock_title),
            value = skinToneLock,
            onValueChange = onSkinToneLockChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_extremes_rolloff_title),
            value = extremesRolloff,
            onValueChange = onExtremesRolloffChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        SzlSlider(
            title = stringResource(R.string.param_spatial_3d_title),
            value = spatial3d,
            onValueChange = onSpatial3dChange,
            min = 0.0f,
            max = 1.0f,
            presets = null
        )

        Spacer(modifier = Modifier.height(AppDimens.ListBottomPadding))
    }
}
