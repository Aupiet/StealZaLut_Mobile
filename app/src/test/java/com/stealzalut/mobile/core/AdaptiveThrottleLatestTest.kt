package com.stealzalut.mobile.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class AdaptiveThrottleLatestTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun `single submission executes and updates latest completed request id`() = testScope.runTest {
        val executedRequests = mutableListOf<String>()
        val throttler = AdaptiveThrottleLatest<String>(
            scope = this,
            minIntervalMs = 50L,
            maxIntervalMs = 500L,
            initialIntervalMs = 100L
        ) { req, _ ->
            executedRequests.add(req)
            60L
        }

        val reqId = throttler.submit("request-1")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("request-1"), executedRequests)
        assertEquals(reqId, throttler.latestCompletedRequestId)
    }

    @Test
    fun `rapid emissions during in-flight execution only execute latest trailing request`() = testScope.runTest {
        val executedRequests = mutableListOf<String>()
        val inFlightCounter = AtomicInteger(0)
        var maxInFlight = 0

        val throttler = AdaptiveThrottleLatest<String>(
            scope = this,
            minIntervalMs = 50L,
            maxIntervalMs = 500L,
            initialIntervalMs = 100L
        ) { req, _ ->
            val count = inFlightCounter.incrementAndGet()
            if (count > maxInFlight) maxInFlight = count
            delay(100L) // Simulate 100ms Python render
            executedRequests.add(req)
            inFlightCounter.decrementAndGet()
            100L
        }

        // Emit initial request
        throttler.submit("P_1")
        // Emit rapid intermediate requests while P_1 is in-flight
        throttler.submit("P_2")
        throttler.submit("P_3")
        throttler.submit("P_4") // Latest trailing

        // Concurrency must never exceed 1
        assertEquals(1, maxInFlight)

        // Advance through all delayed tasks
        testScheduler.advanceUntilIdle()

        // Intermediate P_2 and P_3 must have been skipped; only P_1 and latest trailing P_4 ran
        assertEquals(listOf("P_1", "P_4"), executedRequests)
        assertEquals(1, maxInFlight)
    }

    @Test
    fun `adaptive interval dynamically updates based on execution latency within bounds`() = testScope.runTest {
        var simulatedLatencyMs = 40L // 40 * 2.5 = 100ms -> within [50, 400]
        val throttler = AdaptiveThrottleLatest<String>(
            scope = this,
            minIntervalMs = 50L,
            maxIntervalMs = 400L,
            initialIntervalMs = 150L,
            intervalMultiplier = 2.5
        ) { _, _ ->
            simulatedLatencyMs
        }

        throttler.submit("fast-req")
        testScheduler.advanceUntilIdle()
        assertEquals(100L, throttler.currentIntervalMs)

        // Very slow render (200ms * 2.5 = 500ms -> bounded to max 400ms)
        simulatedLatencyMs = 200L
        throttler.submit("slow-req")
        testScheduler.advanceUntilIdle()
        assertEquals(400L, throttler.currentIntervalMs)

        // Very fast render (10ms * 2.5 = 25ms -> bounded to min 50ms)
        simulatedLatencyMs = 10L
        throttler.submit("ultra-fast-req")
        testScheduler.advanceUntilIdle()
        assertEquals(50L, throttler.currentIntervalMs)
    }

    @Test
    fun `flush triggers execution of pending trailing request immediately`() = testScope.runTest {
        val executed = mutableListOf<String>()
        val throttler = AdaptiveThrottleLatest<String>(
            scope = this,
            minIntervalMs = 100L,
            maxIntervalMs = 500L,
            initialIntervalMs = 100L
        ) { req, _ ->
            executed.add(req)
            50L
        }

        throttler.submit("initial")
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("initial"), executed)

        throttler.submit("trailing")
        throttler.flush()
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("initial", "trailing"), executed)
    }
}
