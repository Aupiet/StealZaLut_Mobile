package com.stealzalut.mobile.ui.screens.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.AppConstants
import com.stealzalut.mobile.core.AppDimens
import com.stealzalut.mobile.data.model.ColorGradingModel
import com.stealzalut.mobile.ui.components.SzlBipolarSlider
import com.stealzalut.mobile.ui.components.SzlSlider
import com.stealzalut.mobile.ui.theme.ColorCardReference
import com.stealzalut.mobile.ui.theme.LocalFontSpec
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Professional 3-Way Color Grading Develop Panel.
 * Uncrowded, comfortable layout with tabs:
 * - 0: Ombres (Shadows)
 * - 1: Tons Moyens (Midtones)
 * - 2: Hautes Lum. (Highlights)
 * - 3: Global
 * - 4: Balance & Fusion (Blending / Balance)
 * Features large chromatic wheel, live tint swatch, luminance slider, and haptics.
 */
@Composable
fun EditorColorGradingPanel(
    colorGrading: ColorGradingModel,
    onUpdateColorGrading: ((ColorGradingModel) -> ColorGradingModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val monoFont = LocalFontSpec.current.mono
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

    val activeTintColor = remember(currentHue, currentSat) {
        if (currentSat <= 0.05f) {
            ColorCardReference.GrayMiddle1
        } else {
            val hsv = floatArrayOf(currentHue, currentSat / 100f, 1.0f)
            Color(android.graphics.Color.HSVToColor(hsv))
        }
    }

    val wheelSpectrum = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    val tabs = listOf(
        stringResource(R.string.wheel_shadows),
        stringResource(R.string.wheel_midtones),
        stringResource(R.string.wheel_highlights),
        stringResource(R.string.wheel_global),
        stringResource(R.string.slider_blending)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.SpacingSmall, vertical = AppDimens.SpacingExtraSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
    ) {
        // --- 1. Ergonomic Scrollable Tab Row ---
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = AppDimens.SpacingSmall,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedTab = index
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(140)).togetherWith(fadeOut(tween(100))) },
            label = "colorGradingTabAnim" // GUARD_ALLOW
        ) { tabIndex ->
            if (tabIndex in 0..3) {
                // --- 2. Chromatic Color Wheel + Swatch + Luminance Slider ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpacingSmall)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppDimens.SpacingExtraSmall),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Generous Interactive Chromatic Color Wheel
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(150.dp)
                        ) {
                            // Outer Halo Ring
                            Canvas(modifier = Modifier.size(150.dp)) {
                                if (currentSat > 1f) {
                                    drawCircle(
                                        color = activeTintColor.copy(alpha = 0.25f),
                                        radius = size.width / 2f + 4f,
                                        style = Stroke(width = 4f)
                                    )
                                }
                            }

                            Canvas(
                                modifier = Modifier
                                    .size(140.dp)
                                    .pointerInput(selectedTab) {
                                        detectTapGestures(
                                            onDoubleTap = {
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
                                        )
                                    }
                                    .pointerInput(selectedTab) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingWheel = true
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            },
                                            onDragEnd = { isDraggingWheel = false },
                                            onDragCancel = { isDraggingWheel = false },
                                            onDrag = { change, _ ->
                                                val radius = size.width / 2f
                                                val center = Offset(radius, radius)
                                                val touchPos = change.position - center
                                                val dist = hypot(touchPos.x, touchPos.y)
                                                val sat = (dist / radius * 100f).coerceIn(0f, 100f)

                                                var angle = Math.toDegrees(atan2(touchPos.y.toDouble(), touchPos.x.toDouble())).toFloat()
                                                if (angle < 0f) angle += 360f

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

                                // 1. Sweep gradient: Full 360 chromatic spectrum
                                drawCircle(
                                    brush = Brush.sweepGradient(colors = wheelSpectrum, center = center),
                                    radius = radius,
                                    center = center
                                )

                                // 2. Radial gradient: Center desaturation to dark canvas
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
                                    color = ColorCardReference.WhiteStep.copy(alpha = 0.35f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = 1.5f)
                                )

                                // Center crosshair dots
                                drawCircle(
                                    color = ColorCardReference.WhiteStep.copy(alpha = 0.5f),
                                    radius = 2.5f,
                                    center = center
                                )

                                // Interactive Thumb Indicator
                                val rad = Math.toRadians(currentHue.toDouble())
                                val dist = (currentSat / 100f) * radius
                                val thumbPos = Offset(
                                    x = center.x + (dist * cos(rad)).toFloat(),
                                    y = center.y + (dist * sin(rad)).toFloat()
                                )

                                val outerR = 10f * reticleRadiusScale
                                val innerR = 7f * reticleRadiusScale
                                drawCircle(ColorCardReference.NeutralDarkBackground, radius = outerR, center = thumbPos)
                                drawCircle(activeTintColor, radius = innerR, center = thumbPos)
                                drawCircle(ColorCardReference.WhiteStep, radius = innerR, center = thumbPos, style = Stroke(width = 2f))
                            }
                        }

                        // Structured Color Grading Info Swatch
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(activeTintColor)
                                        .border(AppDimens.BorderMedium, ColorCardReference.WhiteStep.copy(alpha = 0.6f), CircleShape)
                                )
                                Text(
                                    text = "${currentHue.toInt()}°",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = monoFont),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${currentSat.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = monoFont),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- 3. Active Wheel Luminance Slider ---
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
                        defaultValue = 0.5f
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
