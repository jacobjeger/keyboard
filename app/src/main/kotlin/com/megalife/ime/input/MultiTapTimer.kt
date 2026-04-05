package com.megalife.ime.input

import kotlinx.coroutines.*

/**
 * Manages the multi-tap timeout for T9 input.
 * After the timeout expires, the current character is committed.
 */
class MultiTapTimer(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = 800L,
    private val onTimeout: () -> Unit
) {
    private var job: Job? = null

    /** Start or restart the timer */
    fun restart() {
        job?.cancel()
        job = scope.launch {
            delay(timeoutMs)
            onTimeout()
        }
    }

    /** Cancel the timer (e.g., when a different key is pressed) */
    fun cancel() {
        job?.cancel()
        job = null
    }

    /** Check if the timer is currently running */
    val isActive: Boolean get() = job?.isActive == true
}
