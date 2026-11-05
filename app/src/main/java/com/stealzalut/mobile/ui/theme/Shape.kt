package com.stealzalut.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import com.stealzalut.mobile.core.AppDimens

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(AppDimens.CornerRadiusMicro),
    small = RoundedCornerShape(AppDimens.CornerRadiusSmall),
    medium = RoundedCornerShape(AppDimens.CornerRadiusMedium),
    large = RoundedCornerShape(AppDimens.CornerRadiusNormal),
    extraLarge = RoundedCornerShape(AppDimens.CornerRadiusCard)
)
