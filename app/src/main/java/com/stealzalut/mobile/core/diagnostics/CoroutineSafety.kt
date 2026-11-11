package com.stealzalut.mobile.core.diagnostics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.stealzalut.mobile.R
import com.stealzalut.mobile.core.UiText

suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }

object DiagnosticUiEvents {
    private val mutableEvents = MutableSharedFlow<UiText>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = mutableEvents.asSharedFlow()

    fun reportUnexpectedError() {
        mutableEvents.tryEmit(UiText.Resource(R.string.err_unexpected_operation))
    }
}

fun CoroutineScope.launchSafely(
    tag: String = "Coroutine",
    onError: (Throwable) -> Unit = { DiagnosticUiEvents.reportUnexpectedError() },
    block: suspend CoroutineScope.() -> Unit
): Job = launch {
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (throwable: Throwable) {
        AppLog.log(LogLevel.ERROR, tag, "Coroutine failed.", throwable)
        try {
            onError(throwable)
        } catch (callbackError: Throwable) {
            AppLog.log(LogLevel.ERROR, tag, "Coroutine error callback failed.", callbackError)
        }
    }
}
