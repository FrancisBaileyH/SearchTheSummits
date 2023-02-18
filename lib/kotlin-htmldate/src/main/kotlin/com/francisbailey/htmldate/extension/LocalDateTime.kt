package com.francisbailey.htmldate.extension

import java.time.DateTimeException
import java.time.LocalDateTime

/**
 * Convenience methods for creating LocalDateTime instances
 */
class LocalDateTimeBuilder {

    companion object {
        private val DATE_COMPONENT_RANGE = (1..6)
        private const val MONTH_PADDING = 6
        private const val DAY_PADDING = 15

        /**
         * Pad the dates except for the year
         */
        fun of(year: Int, month: Int = MONTH_PADDING, day: Int = DAY_PADDING, hour: Int = 0, minute: Int = 0, second: Int = 0): LocalDateTime {
            return LocalDateTime.of(year, month, day, hour, minute, second)
        }

        fun of(vararg dateParts: Int): LocalDateTime {
            if (dateParts.size !in DATE_COMPONENT_RANGE) {
                throw DateTimeException("Incorrect number of arguments: $dateParts")
            }

            val year = dateParts[0]
            val month = dateParts.getOrElse(1) { MONTH_PADDING }
            val day = dateParts.getOrElse(2) { DAY_PADDING }
            val hour = dateParts.getOrElse(3) { 0 }
            val minute = dateParts.getOrElse(4) { 0 }
            val seconds = dateParts.getOrElse(5) { 0 }

            return LocalDateTime.of(year, month, day, hour, minute, seconds)
        }
    }
}


fun LocalDateTime?.setOrOverrideIf(value: LocalDateTime?, check: (original: LocalDateTime, new: LocalDateTime) -> Boolean): LocalDateTime? {
    return when {
        this == null -> value
        value == null -> this
        check(this, value) -> value
        else -> this
    }
}