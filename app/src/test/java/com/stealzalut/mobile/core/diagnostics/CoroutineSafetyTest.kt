package com.stealzalut.mobile.core.diagnostics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoroutineSafetyTest {
    @Test
    fun `suspendRunCatching returns successful values`() = runBlocking {
        assertEquals(Result.success(42), suspendRunCatching { 42 })
    }

    @Test
    fun `suspendRunCatching captures errors`() = runBlocking {
        val result = suspendRunCatching<Int> { throw OutOfMemoryError("test") }
        assertTrue(result.exceptionOrNull() is OutOfMemoryError)
    }

    @Test
    fun `suspendRunCatching rethrows cancellation`() = runBlocking {
        assertThrows(CancellationException::class.java) {
            runBlocking { suspendRunCatching<Int> { throw CancellationException("cancelled") } }
        }
    }
}
