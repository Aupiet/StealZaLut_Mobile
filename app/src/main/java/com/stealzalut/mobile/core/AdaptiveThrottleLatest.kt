package com.stealzalut.mobile.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

/**
 * Adaptive "latest" throttler replacing fixed-delay debounce for real-time preview pipelines.
 *
 * Solves the continuous touch problem of fixed `.debounce(200L)`:
 * 1. At most ONE computation job in-flight at any time.
 * 2. Only the latest request is retained while a job is running; intermediate requests are dropped.
 * 3. Adaptive throttle interval dynamically scaled to 2.0x - 3.0x of the last measured computation latency (bounded).
 * 4. Trailing execution: the final request after a continuous gesture is guaranteed to be processed.
 * 5. Monotonically increasing request sequence IDs to ignore superseded / stale results.
 */
class AdaptiveThrottleLatest<T : Any>(
    private val scope: CoroutineScope,
    val minIntervalMs: Long = 80L,
    val maxIntervalMs: Long = 600L,
    initialIntervalMs: Long = 150L,
    val intervalMultiplier: Double = 2.5,
    private val execute: suspend (request: T, requestId: Long) -> Long? // returns executionTimeMs
) {
    private val mutex = Mutex()
    private val nextRequestId = AtomicLong(0L)

    @Volatile
    var latestCompletedRequestId: Long = 0L
        private set

    @Volatile
    var currentIntervalMs: Long = initialIntervalMs
        private set

    private var inFlight = false
    private var pendingRequest: T? = null
    private var pendingRequestId: Long = 0L
    private var lastExecutionStartTime = 0L
    private var loopJob: Job? = null

    /**
     * Submits a new request. If an execution is in-flight, replaces any pending request
     * so that only the latest parameters will be processed next.
     */
    fun submit(request: T): Long {
        val reqId = nextRequestId.incrementAndGet()
        scope.launch {
            mutex.withLock {
                pendingRequest = request
                pendingRequestId = reqId
                if (!inFlight) {
                    startProcessingLoopLocked()
                }
            }
        }
        return reqId
    }

    private fun startProcessingLoopLocked() {
        inFlight = true
        loopJob = scope.launch {
            while (true) {
                val req: T
                val reqId: Long
                val timeToWait: Long

                mutex.withLock {
                    val pReq = pendingRequest
                    val pId = pendingRequestId
                    if (pReq == null) {
                        inFlight = false
                        return@launch
                    }
                    req = pReq
                    reqId = pId
                    pendingRequest = null

                    // Throttle: ensure at least currentIntervalMs between consecutive executions
                    val elapsedSinceStart = System.currentTimeMillis() - lastExecutionStartTime
                    timeToWait = (currentIntervalMs - elapsedSinceStart).coerceAtLeast(0L)
                }

                if (timeToWait > 0L) {
                    delay(timeToWait)
                }

                lastExecutionStartTime = System.currentTimeMillis()
                val execTimeMs = try {
                    execute(req, reqId)
                } catch (t: Throwable) {
                    null
                }

                mutex.withLock {
                    if (reqId > latestCompletedRequestId) {
                        latestCompletedRequestId = reqId
                    }
                    if (execTimeMs != null && execTimeMs > 0L) {
                        currentIntervalMs = (execTimeMs * intervalMultiplier).roundToLong()
                            .coerceIn(minIntervalMs, maxIntervalMs)
                    }
                    // If no new pendingRequest arrived while executing, exit loop
                    if (pendingRequest == null) {
                        inFlight = false
                        return@launch
                    }
                }
            }
        }
    }

    /**
     * Immediately triggers processing of any pending request (e.g. upon gesture release).
     */
    fun flush() {
        scope.launch {
            mutex.withLock {
                if (!inFlight && pendingRequest != null) {
                    startProcessingLoopLocked()
                }
            }
        }
    }
}
