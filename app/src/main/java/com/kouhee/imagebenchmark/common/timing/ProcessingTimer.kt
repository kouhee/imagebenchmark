package com.kouhee.imagebenchmark.common.timing

import android.os.SystemClock

internal data class ClockMark(
    val elapsedRealtimeNs: Long,
    val nanoTimeNs: Long
)

internal enum class TimingSource {
    ELAPSED_REALTIME,
    NANO_TIME_FALLBACK,
    MINIMUM_CLAMP
}

internal data class TimingResult(
    val timeUs: Double,
    val source: TimingSource
) {
    val timeMs: Double
        get() = timeUs / 1000.0
}

internal object ProcessingTimer {
    fun mark(): ClockMark = ClockMark(
        elapsedRealtimeNs = SystemClock.elapsedRealtimeNanos(),
        nanoTimeNs = System.nanoTime()
    )

    fun durationUs(start: ClockMark, end: ClockMark): TimingResult {
        val elapsedDiff = end.elapsedRealtimeNs - start.elapsedRealtimeNs
        if (elapsedDiff > 0L) {
            return TimingResult(elapsedDiff / 1000.0, TimingSource.ELAPSED_REALTIME)
        }

        val nanoDiff = end.nanoTimeNs - start.nanoTimeNs
        if (nanoDiff > 0L) {
            return TimingResult(nanoDiff / 1000.0, TimingSource.NANO_TIME_FALLBACK)
        }

        return TimingResult(1.0, TimingSource.MINIMUM_CLAMP)
    }
}
