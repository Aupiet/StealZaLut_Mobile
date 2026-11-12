package com.stealzalut.mobile.ui.screens.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ZonePaletteAnalysis

@Composable
fun ZonePaletteVisualization(
    source: ZonePaletteAnalysis?,
    reference: ZonePaletteAnalysis?
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)) {
        listOfNotNull(
            source?.let { "Source" to it },
            reference?.let { "Reference" to it }
        ).forEach { (label, palette) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    modifier = Modifier.width(64.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                        .clip(RoundedCornerShape(AppDimens.CornerRadiusSmall))
                ) {
                    palette.zones.forEach { zone ->
                        Box(
                            modifier = Modifier
                                .weight(zone.area.coerceAtLeast(0.01f))
                                .fillMaxHeight()
                                .background(Color(android.graphics.Color.parseColor(zone.hex)))
                        )
                    }
                }
            }
        }
    }
}
