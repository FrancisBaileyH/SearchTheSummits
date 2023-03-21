package com.francisbailey.summitsearch.services.common

import org.springframework.stereotype.Service
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource.Monotonic.ValueTimeMark
import kotlin.time.TimeSource.Monotonic

@Service
class MonotonicClock {

    @OptIn(ExperimentalTime::class)
    fun now(): TimeMark {
        return TimeMark(Monotonic.markNow())
    }

    @OptIn(ExperimentalTime::class)
    fun timeSince(timeMark: TimeMark): Duration {
        return timeMark.wrappedTimeMark.elapsedNow()
    }

    data class TimeMark @OptIn(ExperimentalTime::class) constructor(
        val wrappedTimeMark: ValueTimeMark
    )
}

