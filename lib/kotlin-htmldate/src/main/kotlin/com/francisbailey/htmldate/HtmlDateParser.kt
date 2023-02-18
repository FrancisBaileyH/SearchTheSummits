package com.francisbailey.htmldate

import com.francisbailey.htmldate.extractor.DateExtractorPatterns
import com.francisbailey.htmldate.extractor.DateExtractorStrategy
import java.time.DateTimeException
import java.time.LocalDateTime

class HtmlDateParser(
    private val extractors: List<DateExtractorStrategy>
) {

    fun parse(value: String): LocalDateTime? {
        if (!isDateCandidate(value)) {
            return null
        }

        return extractors.firstNotNullOfOrNull {
            try {
                it.find(value)
            } catch (e: DateTimeException) {
                null
            }
        }
    }

    private fun isDateCandidate(value: String): Boolean {
        val digitCount = value.count { it.isDigit() }

        return when {
            value.length < 6 -> false
            digitCount !in (4..18) -> false
            !DateExtractorPatterns.TEXT_DATE_PATTERN.matcher(value).find() -> false
            DateExtractorPatterns.NO_TEXT_DATE_PATTERN.matcher(value).matches() -> false
            else -> true
        }
    }

}