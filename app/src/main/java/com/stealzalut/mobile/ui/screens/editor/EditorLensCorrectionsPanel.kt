package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.LensCorrectionsModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider

/**
 * Lightroom Classic Lens Corrections Develop Panel.
 * Supports Geometric Distortion (Pincushion / Barrel),
 * Chromatic Aberration removal & defringe (Red/Cyan, Blue/Yellow),
 * and Lens Vignetting (Amount, Midpoint).
 */
@Composable
fun EditorLensCorrectionsPanel(
    lensCorrections: LensCorrectionsModel,
    onUpdateLensCorrections: ((LensCorrectionsModel) -> LensCorrectionsModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        // --- 1. Distortion ---
        SzlBipolarSlider(
            title = stringResource(R.string.slider_distortion),
            value = lensCorrections.distortion,
            onValueChange = { newVal -> onUpdateLensCorrections { it.copy(distortion = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        // --- 2. Chromatic Aberration Switch ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chk_remove_ca),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = lensCorrections.removeCa,
                onCheckedChange = { checked -> onUpdateLensCorrections { it.copy(removeCa = checked) } }
            )
        }

        if (lensCorrections.removeCa) {
            SzlBipolarSlider(
                title = stringResource(R.string.slider_ca_red_cyan),
                value = lensCorrections.caRedCyan,
                onValueChange = { newVal -> onUpdateLensCorrections { it.copy(caRedCyan = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )

            SzlBipolarSlider(
                title = stringResource(R.string.slider_ca_blue_yellow),
                value = lensCorrections.caBlueYellow,
                onValueChange = { newVal -> onUpdateLensCorrections { it.copy(caBlueYellow = newVal) } },
                min = -100.0f,
                max = 100.0f,
                defaultValue = 0.0f
            )
        }

        // --- 3. Lens Vignetting ---
        SzlBipolarSlider(
            title = stringResource(R.string.slider_lens_vignette),
            value = lensCorrections.vignetteAmount,
            onValueChange = { newVal -> onUpdateLensCorrections { it.copy(vignetteAmount = newVal) } },
            min = -100.0f,
            max = 100.0f,
            defaultValue = 0.0f
        )

        SzlSlider(
            title = stringResource(R.string.slider_midpoint),
            value = lensCorrections.vignetteMidpoint / 100.0f,
            onValueChange = { newVal -> onUpdateLensCorrections { it.copy(vignetteMidpoint = newVal * 100.0f) } },
            min = 0.0f,
            max = 1.0f,
            step = 0.02f,
            presets = null
        )
    }
}
