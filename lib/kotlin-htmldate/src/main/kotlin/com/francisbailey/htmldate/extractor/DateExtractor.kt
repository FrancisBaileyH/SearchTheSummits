package com.francisbailey.htmldate.extractor

import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import com.francisbailey.htmldate.extension.hasGroupCount
import com.francisbailey.htmldate.extension.intParts
import java.time.LocalDateTime


internal class HeuristicDateCorrector {

    companion object {

        fun fixYear(year: Int): Int = when (year) {
            in 90..100 -> year + 1900
            in 0..89 -> year + 2000
            else -> year
        }

        fun tryDayAndMonthSwap(day: Int, month: Int): Pair<Int, Int> {
            return if (day <= 12 && month <= 12) {
                month to day
            } else {
                day to month
            }
        }
    }
}


interface DateExtractorStrategy {
    fun find(value: String): LocalDateTime?
}

/**
 * Extract dates in the form:
 * - https://test.com/2022-02-04/some-page
 * - https://test.com/2022/02/04/some-page
 */
class UrlDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.COMPLETE_URL.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        return LocalDateTimeBuilder.of(*matcher.intParts(3))
    }
}

/**
 * Extract dates in the form:
 * - https://test.com/2022-02/some-page
 * - https://test.com/2022/02/some-page
 */
class PartialUrlDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.PARTIAL_URL.matcher(value)

        if (!matcher.hasGroupCount(2)) {
            return null
        }

        return LocalDateTimeBuilder.of(*matcher.intParts(2))
    }
}

/**
 * Extract dates in the form:
 * - 20220204
 *
 * (without regex)
 */
class NoRegexYMDNoSeparatorDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val digitCount = value
            .take(8)
            .count { it.isDigit() }

        if (digitCount < 8) {
            return null
        }

        val year = value.substring(0..3).toInt()
        val month = value.substring(4..5).toInt()
        val day = value.substring(6..7).toInt()

        return LocalDateTimeBuilder.of(year, month, day)
    }
}

/**
 * Extract dates in the form:
 * - 20220204
 */
class YYYYMMDDNoSeparatorDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.YMD_NO_SEP_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(1)) {
            return null
        }

        val match = matcher.group(1)

        val year =  match.substring(0..3).toInt()
        val month = match.substring(4..5).toInt()
        val day =   match.substring(6..7).toInt()

        return LocalDateTimeBuilder.of(year, month, day)
    }
}

/**
 * Extract dates in the form:
 * - 1990-02-01
 */
class YMDDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.YMD_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        return LocalDateTimeBuilder.of(*matcher.intParts(3))
    }
}

/**
 * Extract dates in the form:
 * - 01-02-1990 // 1990-02-01
 * - 01-02-90 // 1990-02-01
 * - 01-02-01 // 2001-02-01
 */
class DMYDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.DMY_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        val year = HeuristicDateCorrector.fixYear(matcher.group(3).toInt())

        val monthDayPair = HeuristicDateCorrector.tryDayAndMonthSwap(
            day = matcher.group(1).toInt(),
            month = matcher.group(2).toInt()
        )

        return LocalDateTimeBuilder.of(year, monthDayPair.first, monthDayPair.second)
    }
}

/**
 * Same pattern as DMY. In cases where the value is MDY, it will be caught by DMY first,
 * but if the value is greater than the current time, it will fallback to this value instead
 * Not ideal, but it's the best we can do with heuristics.
 */
class MDYDateExtractor: DateExtractorStrategy {
    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.DMY_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        val year = HeuristicDateCorrector.fixYear(matcher.group(3).toInt())
        val month = matcher.group(1).toInt()
        val day = matcher.group(2).toInt()

        return LocalDateTimeBuilder.of(year, month, day)
    }
}

/**
 * Extract dates in the form:
 * - 1990-02-01
 */
class YMDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.YM_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(2)) {
            return null
        }

        return LocalDateTimeBuilder.of(*matcher.intParts(2))
    }

}

class MYDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.MY_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(2)) {
            return null
        }

        return LocalDateTimeBuilder.of(*matcher.intParts(2).reversedArray())
    }

}

/**
 * Extract dates in the form
 * - January 10 2023
 * - January 10 23
 * - January 10 93
 */
class LongMDYDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.LONG_MDY_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        val month = matcher.group(1)
        val day = matcher.group(2).toInt()
        val year = HeuristicDateCorrector.fixYear(matcher.group(3).toInt())

        return DateExtractorPatterns.MONTH_NUMBER_MAP[month]?.let { mappedMonthValue ->
            LocalDateTimeBuilder.of(year = year, month = mappedMonthValue,  day = day)
        }
    }
}

class LongDMYDateExtractor: DateExtractorStrategy {

    override fun find(value: String): LocalDateTime? {
        val matcher = DateExtractorPatterns.LONG_DMY_PATTERN.matcher(value)

        if (!matcher.hasGroupCount(3)) {
            return null
        }

        val month = matcher.group(2)
        val day = matcher.group(1).toInt()
        val year = HeuristicDateCorrector.fixYear(matcher.group(3).toInt())

        return DateExtractorPatterns.MONTH_NUMBER_MAP[month]?.let { mappedMonthValue ->
            LocalDateTimeBuilder.of(year = year, month = mappedMonthValue,  day = day)
        }
    }
}

