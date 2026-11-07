package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Professional 3-Way Color Grading Develop Panel (Shadows, Midtones, Highlights, Global, Blending & Balance).
 * Features:
 * - Dedicated collision-free geometry with separate padding for the chromatic wheel and luminance slider.
 * - Tabbed navigation separating the 3-Way zones from the Blending/Balance controls for zero vertical scrolling.
 * - Reactive outer halo ring reflecting the selected hue and saturation.
 * - Touch-sensitive reticle indicator with smooth scale expansion on interaction.
 * - Double-tap reset on the wheel and reticle (resets Hue & Saturation to 0) with tactile haptic feedback.
 * - Double-tap / single tap reset on all sliders.
 * - Live color swatch badge with structured visual hierarchy (Hue in degrees, Saturation %).
 */
@Composable
fun EditorColorGradingPanel(
    colorGrading: ColorGradingModel,
    onUpdateColorGrading: ((ColorGradingModel) -> ColorGradingModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Shadows, 1 = Midtones, 2 = Highlights, 3 = Global, 4 = Blending/Balance
    var isDraggingWheel by remember { mutableStateOf(false) }

    val reticleRadiusScale by animateFloatAsState(
        targetValue = if (isDraggingWheel) 1.35f else 1.0f,
        label = "reticleScale" // GUARD_ALLOW
    )

    val currentHue = when (selectedTab) {
        0 -> colorGrading.shadowHue
        1 -> colorGrading.midtoneHue
        2 -> colorGrading.highlightHue
        else -> colorGrading.globalHue
    }
    val currentSat = when (selectedTab) {
        0 -> colorGrading.shadowSat
        1 -> colorGrading.midtoneSat
        2 -> colorGrading.highlightSat
        else -> colorGrading.globalSat
    }
    val currentLum = when (selectedTab) {
        0 -> colorGrading.shadowLum
        1 -> colorGrading.midtoneLum
        2 -> colorGrading.highlightLum
        else -> colorGrading.globalLum
    }

    fun hslToColor(hue: Float, satPercent: Float, lumPercent: Float): Color {
        val s = (satPercent / 100f).coerceIn(0f, 1f)
        if (s < 0.01f) return ColorCardReference.GrayMiddle2
        val l = (0.5f + (lumPercent / 100f) * 0.25f).coerceIn(0.1f, 0.9f)
        val c = (1f - abs(2f * l - 1f)) * s
        val hPrime = (hue % 360f) / 60f
        val x = c * (1f - abs((hPrime % 2f) - 1f))
        val m = l - c / 2f
        val (r, g, b) = when {
            hPrime < 1f -> Triple(c, x, 0f)
            hPrime < 2f -> Triple(x, c, 0f)
            hPrime < 3f -> Triple(0f, c, x)
            hPrime < 4f -> Triple(0f, x, c)
            hPrime < 5f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r + m, g + m, b + m)
    }

    val activeTintColor = hslToColor(currentHue, currentSat, currentLum)

    val wheelSpectrum = listOf(
        ColorCardReference.HslRed,
        ColorCardReference.HslOrange,
        ColorCardReference.HslYellow,
        ColorCardReference.HslGreen,
        ColorCardReference.HslAqua,
        ColorCardReference.HslBlue,
        ColorCardReference.HslPurple,
        ColorCardReference.HslMagenta,
        ColorCardReference.HslRed
    )

    fun resetActiveWheel() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onUpdateColorGrading { curr ->
            when (selectedTab) {
                0 -> curr.copy(shadowHue = 0f, shadowSat = 0f)
                1 -> curr.copy(midtoneHue = 0f, midtoneSat = 0f)
                2 -> curr.copy(highlightHue = 0f, highlightSat = 0f)
                else -> curr.copy(globalHue = 0f, globalSat = 0f)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AppDimens.SpacingMicro),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingExtraSmall)
    ) {
        // --- 1. Five Navigation Tabs: Shadows | Midtones | Highlights | Global | Blending & Balance ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = AppDimens.SpacingNone,
            modifier = Modifier.fillMaxWidth()
        ) {
            val tabs = listOf(
                Pair(R.string.wheel_shadows, hslToColor(colorGrading.shadowHue, colorGrading.shadowSat, colorGrading.shadowLum)),
                Pair(R.string.wheel_midtones, hslToColor(colorGrading.midtoneHue, colorGrading.midtoneSat, colorGrading.midtoneLum)),
                Pair(R.string.wheel_highlights, hslToColor(colorGrading.highlightHue, colorGrading.highlightSat, colorGrading.highlightLum)),
                Pair(R.string.wheel_global, hslToColor(colorGrading.globalHue, colorGrading.globalSat, colorGrading.globalLum)),
                Pair(R.string.tab_blending_balance, MaterialTheme.colorScheme.primary)
            )

            tabs.forEachIndexed { index, (labelRes, tintCol) ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index < 4) {
                                Box(
                                    modifier = Modifier
                                        .size(AppDimens.SpacingSmall)
                                        .clip(CircleShape)
                                        .background(tintCol)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(AppDimens.IconMicro),
                                    tint = tintCol
                                )
                            }
                            Spacer(modifier = Modifier.width(AppDimens.SpacingExtraSmall))
                            Text(
                                text = stringResource(labelRes),
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
            label = "colorGradingTabTransition" // GUARD_ALLOW
        ) { tabIndex ->
            if (tabIndex in 0..3) {
                // --- 2. Chromatic Wheel & Luminance Controls for Active Zone ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimens.SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Color Wheel with Outer Hue Halo Ring
                        Box(
                            modifier = Modifier.size(AppDimens.ColorWheelZeroScrollSize),
                            contentAlignment = Alignment.Center
                        ) {
                            // Subtle Outer Halo Glow reflecting the selected tint
                            if (currentSat > 1f) {
                                Box(
                                    modifier = Modifier
                                        .size(AppDimens.ColorWheelZeroScrollSize)
                                        .clip(CircleShape)
                                        .background(activeTintColor.copy(alpha = 0.25f))
                                )
                            }

                            Canvas(
                                modifier = Modifier
                                    .size(AppDimens.ColorWheelZeroScrollSize)
                                    .pointerInput(selectedTab) {
                                        fun updateFromOffset(pos: Offset, sizePx: Float) {
                                            val center = sizePx / 2f
                                            val dx = pos.x - center
                                            val dy = pos.y - center
                                            val dist = hypot(dx, dy)
                                            val radius = center
                                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            if (angle < 0) angle += 360f
                                            val sat = (dist / radius * 100f).coerceIn(0f, 100f)

                                            onUpdateColorGrading { curr ->
                                                when (selectedTab) {
                                                    0 -> curr.copy(shadowHue = angle, shadowSat = sat)
                                                    1 -> curr.copy(midtoneHue = angle, midtoneSat = sat)
                                                    2 -> curr.copy(highlightHue = angle, highlightSat = sat)
                                                    else -> curr.copy(globalHue = angle, globalSat = sat)
                                                }
                                            }
                                        }

                                        detectTapGestures(
                                            onDoubleTap = { resetActiveWheel() },
                                            onTap = { offset -> updateFromOffset(offset, size.width.toFloat()) }
                                        )
                                    }
                                    .pointerInput(selectedTab) {
                                        detectDragGestures(
                                            onDragStart = { isDraggingWheel = true },
                                            onDragEnd = { isDraggingWheel = false },
                                            onDragCancel = { isDraggingWheel = false },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val center = size.width / 2f
                                                val dx = change.position.x - center
                                                val dy = change.position.y - center
                                                val dist = hypot(dx, dy)
                                                val radius = center
                                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                                if (angle < 0) angle += 360f
                                                val sat = (dist / radius * 100f).coerceIn(0f, 100f)

                                                onUpdateColorGrading { curr ->
                                                    when (selectedTab) {
                                                        0 -> curr.copy(shadowHue = angle, shadowSat = sat)
                                                        1 -> curr.copy(midtoneHue = angle, midtoneSat = sat)
                                                        2 -> curr.copy(highlightHue = angle, highlightSat = sat)
                                                        else -> curr.copy(globalHue = angle, globalSat = sat)
                                                    }
                                                }
                                            }
                                        )
                                    }
                            ) {
                                val radius = size.width / 2f
                                val center = Offset(radius, radius)

                                // Full 360 chromatic spectrum
                                drawCircle(
                                    brush = Brush.sweepGradient(colors = wheelSpectrum, center = center),
                                    radius = radius,
                                    center = center
                                )

                                // Radial center desaturation to dark neutral
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(ColorCardReference.NeutralDarkBackground, Color.Transparent),
                                        center = center,
                                        radius = radius * 0.95f
                                    ),
                                    radius = radius,
                                    center = center
                                )

                                // Outer border ring
                                drawCircle(
                                    color = ColorCardReference.WhiteStep.copy(alpha = 0.4f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 2f)
                                )

                                // Reticle center neutral anchor
                                drawCircle(
                                    color = ColorCardReference.WhiteStep.copy(alpha = 0.5f),
                                    radius = 3f,
                                    center = center
                                )

                                // Reactive Interactive thumb / reticle
                                val rad = Math.toRadians(currentHue.toDouble())
                                val dist = (currentSat / 100f) * radius
                                val thumbX = center.x + (dist * cos(rad)).toFloat()
                                val thumbY = center.y + (dist * sin(rad)).toFloat()
                                val thumbPos = Offset(thumbX, thumbY)

                                val outerR = 9f * reticleRadiusScale
                                val innerR = 7f * reticleRadiusScale

                                drawCircle(ColorCardReference.NeutralDarkBackground, radius = outerR, center = thumbPos)
                                drawCircle(activeTintColor, radius = innerR, center = thumbPos)
                                drawCircle(ColorCardReference.WhiteStep, radius = innerR, center = thumbPos, style = Stroke(width = 2f))
                            }
                        }

                        // Structured Color Grading Info Card
                        Surface(
                            shape = RoundedCornerShape(AppDimens.CornerRadiusNormal),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppConstants.OPACITY_CONTAINER_MEDIUM),
                            border = androidx.compose.foundation.BorderStroke(
                                AppDimens.BorderThin,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = AppConstants.OPACITY_BORDER_SUBTLE)
                            ),
                            modifier = Modifier.padding(start = AppDimens.SpacingSmall)
                        ) {
                            Column(
                                modifier = Modifier.padding(AppDimens.SpacingMedium),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(AppDimens.CurtainHandleSize)
                                        .clip(CircleShape)
                                        .background(activeTintColor)
                                        .border(AppDimens.BorderMedium, ColorCardReference.WhiteStep.copy(alpha = 0.6f), CircleShape)
                                )
                                Text(
                                    text = stringResource(R.string.color_grading_hue_format, currentHue.toInt()),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.color_grading_sat_format, currentSat.toInt()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- 3. Active Wheel Luminance Slider (Immediately visible without scrolling) ---
                    SzlBipolarSlider(
                        title = stringResource(R.string.slider_luminance),
                        value = currentLum,
                        onValueChange = { newVal ->
                            onUpdateColorGrading { curr ->
                                when (selectedTab) {
                                    0 -> curr.copy(shadowLum = newVal)
                                    1 -> curr.copy(midtoneLum = newVal)
                                    2 -> curr.copy(highlightLum = newVal)
                                    else -> curr.copy(globalLum = newVal)
                                }
                            }
                        },
                        min = -100.0f,
                        max = 100.0f,
                        defaultValue = 0.0f
                    )
                }
            } else {
                // --- 4. Tab 4: Blending and Balance Controls ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingMedium)
                ) {
                    SzlSlider(
                        title = stringResource(R.string.slider_blending),
                        value = colorGrading.blending / 100.0f,
                        onValueChange = { newVal -> onUpdateColorGrading { it.copy(blending = newVal * 100.0f) } },
                        min = 0.0f,
                        max = 1.0f,
                        step = 0.01f,
                        presets = null
                    )

                    SzlBipolarSlider(
                        title = stringResource(R.string.slider_balance),
                        value = colorGrading.balance,
                        onValueChange = { newVal -> onUpdateColorGrading { it.copy(balance = newVal) } },
                        min = -100.0f,
                        max = 100.0f,
                        defaultValue = 0.0f
                    )
                }
            }
        }
    }
}
