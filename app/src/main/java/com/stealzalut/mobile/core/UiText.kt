package com.stealzalut.mobile.core

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * Type-safe representation of text to display in UI.
 * Can be either a static dynamic string or an Android string resource with optional format args.
 * Prevents ViewModels from hardcoding user-facing strings or holding Android Context references.
 */
@Immutable
sealed interface UiText {
    @Immutable
    data class DynamicString(val value: String) : UiText

    @Immutable
    data class Resource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText

    fun asString(context: Context): String = when (this) {
        is DynamicString -> value
        is Resource -> if (args.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *args.toTypedArray())
        }
    }

    @Composable
    fun asString(): String = when (this) {
        is DynamicString -> value
        is Resource -> if (args.isEmpty()) {
            stringResource(resId)
        } else {
            stringResource(resId, *args.toTypedArray())
        }
    }

    companion object {
        fun from(text: String): UiText = DynamicString(text)
        fun from(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, args.toList())
    }
}
