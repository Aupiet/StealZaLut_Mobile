package com.stealzalut.mobile.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.stealzalut.mobile.ui.theme.icons
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorTransferMethod
import com.stealzalut.mobile.data.model.SavedGradedImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImageDetailDialog(
    image: SavedGradedImage,
    onDismiss: () -> Unit,
    onShare: (SavedGradedImage) -> Unit,
    onDelete: (SavedGradedImage) -> Unit
) {
    val context: Context = LocalContext.current
    val scrollState = rememberScrollState()
    val dateStr = SimpleDateFormat("dd MMMM yyyy à HH:mm", Locale.getDefault()).format(Date(image.timestamp))

    val fullBitmap by produceState<Bitmap?>(initialValue = null, image.filePath) {
        value = withContext(Dispatchers.IO) {
            try {
                val f = File(image.filePath)
                if (f.exists()) {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, opts)
                    if (opts.outWidth <= 0 || opts.outHeight <= 0) return@withContext null
                    var sample = 1
                    val maxDim = maxOf(opts.outWidth, opts.outHeight)
                    while (maxDim / (sample * 2) >= 1440) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeFile(f.absolutePath, decodeOpts)
                } else null
            } catch (t: Throwable) {
                null
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(AppDimens.SpacingSpacious),
            shape = RoundedCornerShape(AppDimens.CornerRadiusLarge),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AppDimens.DialogElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimens.SpacingNormal)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = MaterialTheme.icons.statusInfo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconNormal)
                        )
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(
                            text = image.themeName ?: stringResource(R.string.detail_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = MaterialTheme.icons.actionClose,
                            contentDescription = stringResource(R.string.detail_close)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingSmall))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                ) {
                    // Full Photo Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimens.PhotoDetailImageMaxHeight)
                            .clip(RoundedCornerShape(AppDimens.CornerRadiusLarge))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (fullBitmap != null) {
                            Image(
                                bitmap = fullBitmap!!.asImageBitmap(),
                                contentDescription = image.themeName ?: stringResource(R.string.detail_title),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Metadata Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_SUBTLE)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.SpacingNormal),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.detail_info_title),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.detail_resolution_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (image.width > 0) "${image.width} × ${image.height} px" else stringResource(R.string.detail_high_res),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.detail_date_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                            }
                            val unknownStr = stringResource(R.string.detail_unknown)
                            val fileSize = remember(image.filePath) {
                                val f = File(image.filePath)
                                if (f.exists()) String.format(Locale.getDefault(), "%.2f Mo", f.length() / (1024.0 * 1024.0)) else unknownStr
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.detail_file_size_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(fileSize, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    // Preset / Look Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM)
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier.padding(AppDimens.SpacingNormal),
                            verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = MaterialTheme.icons.toolColorMixer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(AppDimens.IconSmall)
                                )
                                Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                                Text(
                                    text = stringResource(R.string.detail_preset_used, image.themeName ?: stringResource(R.string.detail_default_look)),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val methodObj = ColorTransferMethod.fromKey(image.method ?: "hm-mkl-hm")
                            Text(
                                text = stringResource(R.string.detail_algorithm, stringResource(methodObj.displayNameRes), (image.intensity * 100).toInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (image.paletteHexes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(AppDimens.SpacingExtraSmall))
                                PaletteSwatchesView(
                                    title = stringResource(R.string.detail_swatches_title),
                                    colors = image.paletteHexes
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimens.SpacingNormal))

                // Bottom Actions: Share and Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                ) {
                    OutlinedButton(
                        onClick = { onShare(image) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal)
                    ) {
                        Icon(imageVector = MaterialTheme.icons.actionShare, contentDescription = null, modifier = Modifier.size(AppDimens.IconNormal))
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(text = stringResource(R.string.detail_btn_share), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDelete(image) },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = AppDimens.MinTouchTarget),
                        shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = MaterialTheme.icons.actionDelete, contentDescription = null, modifier = Modifier.size(AppDimens.IconNormal))
                        Spacer(modifier = Modifier.width(AppDimens.SpacingSmall))
                        Text(text = stringResource(R.string.detail_btn_delete), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
