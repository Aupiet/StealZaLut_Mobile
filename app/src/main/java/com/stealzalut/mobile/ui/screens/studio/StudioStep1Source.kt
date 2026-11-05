package com.stealzalut.mobile.ui.screens.studio

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.ui.components.ImagePickerCard

@Composable
fun StudioStep1Source(
    sourceBitmap: Bitmap?,
    onImageSelected: (Uri) -> Unit,
    canProceed: Boolean,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
    ) {
        SectionTitleBanner(
            title = stringResource(R.string.wizard_step_1_title),
            subtitle = stringResource(R.string.wizard_step_1_subtitle)
        )

        ImagePickerCard(
            title = stringResource(R.string.wizard_step_1_title),
            subtitle = stringResource(R.string.wizard_step_1_empty),
            badgeText = stringResource(R.string.wizard_step_1_badge),
            bitmap = sourceBitmap,
            onImageSelected = onImageSelected
        )

        Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = AppDimens.ButtonHeightStandard),
            shape = RoundedCornerShape(AppDimens.CornerRadiusCard)
        ) {
            Text(
                text = stringResource(R.string.wizard_step_1_btn_next),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(AppDimens.IconNormal)
            )
        }
    }
}
