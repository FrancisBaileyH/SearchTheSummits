package com.francisbailey.htmldate.extractor

import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URL
import java.time.DateTimeException
import java.time.LocalDateTime

class DateExtractorTest {

    @Test
    fun `retrieves date formatted in url as yyyy mm dd`() {
        val extractor = UrlDateExtractor()
        val expectedDate = LocalDateTime.of(2022, 2, 1, 0, 0, 0)

        val expectedMatches = listOf(
            URL("https://francisbaileyh.com/2022/02/01/some-article"),
            URL("https://francisbaileyh.com/2022-02-01/some-article")
        )

        val expectedNonMatches = listOf(
            URL("https://francisbaileyh.com/2022/02/some-article"),
            URL("https://francisbaileyh.com/2022-02/some-article"),
            URL("https://francisbaileyh.com/01/02/2022/some-article"),
            URL("https://francisbaileyh.com/01-02-2022/some-article")
        )

        expectedMatches.forEach {
            assertEquals(expectedDate, extractor.find(it.toString()))
        }

        expectedNonMatches.forEach {
            assertNull(extractor.find(it.toString()))
        }
    }

    @Test
    fun `retrieves partial url date from url`() {
        val extractor = PartialUrlDateExtractor()
        val expectedDate = LocalDateTime.of(2022, 2, 15, 0, 0, 0)

        val expectedMatches = listOf(
            URL("https://francisbaileyh.com/2022/02/some-article"),
            URL("https://francisbaileyh.com/2022-02/some-article")
        )

        val expectedNonMatches = listOf(
            URL("https://francisbaileyh.com/01/some-article"),
            URL("https://francisbaileyh.com/01/some-article")
        )

        expectedMatches.forEach {
            assertEquals(expectedDate, extractor.find(it.toString()))
        }

        expectedNonMatches.forEach {
            assertNull(extractor.find(it.toString()))
        }
    }

    @Test
    fun `retrieves date from in the form 20220401`() {
        val extractor = NoRegexYMDNoSeparatorDateExtractor()
        val expectedDate = LocalDateTime.of(2022, 4, 1, 0, 0, 0)

        val expectedMatches = listOf(
            "20220401"
        )

        val expectedNonMatches = listOf(
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedMatches.forEach {
            assertEquals(expectedDate, extractor.find(it))
        }

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `retrieves date from in the form 20220401 with regex`() {
        val extractor = YYYYMMDDNoSeparatorDateExtractor()
        val expectedDate = LocalDateTime.of(2022, 4, 1, 0, 0, 0)

        val expectedMatches = listOf(
            "This is a date of: 20220401",
            "20220401"
        )

        val expectedNonMatches = listOf(
            "2022041",
            "202204"
        )

        expectedMatches.forEach {
            assertEquals(expectedDate, extractor.find(it))
        }

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }

        assertThrows<DateTimeException> { extractor.find("20224011") }
    }

    @Test
    fun `retrieves date from in the form Y-M-D`() {
        val extractor = YMDDateExtractor()
        val expectedDate = LocalDateTime.of(2022, 4, 1, 0, 0, 0)

        val expectedMatches = listOf(
            "2022-04-01",
            "2022-4-1"
        )

        val expectedNonMatches = listOf(
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedMatches.forEach {
            assertEquals(expectedDate, extractor.find(it))
        }

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `retrieves date from in the form D-M-Y`() {
        val extractor = DMYDateExtractor()

        assertEquals(LocalDateTimeBuilder.of(2022, 4, 1), extractor.find("1-4-22"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 1), extractor.find("1-4-93"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 1), extractor.find("1.4.93"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 1), extractor.find("1/4/93"))
        assertEquals(LocalDateTimeBuilder.of(2022, 12, 15), extractor.find("12-15-22"))
        assertEquals(LocalDateTimeBuilder.of(2022, 4, 1), extractor.find("01-04-2022"))

        val expectedNonMatches = listOf(
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `retrieves date from in the form Y-M`() {
        val extractor = YMDateExtractor()

        assertEquals(LocalDateTimeBuilder.of(2022, 4, 15), extractor.find("2022-4"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("1993-4"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("1993.4"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("1993/4"))
        assertEquals(LocalDateTimeBuilder.of(2022, 12, 15), extractor.find("2022-12"))
        assertEquals(LocalDateTimeBuilder.of(2022, 4, 15), extractor.find("2022-04"))

        val expectedNonMatches = listOf(
            "22-03",
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `retrieves date from in long form of month day year`() {
        val extractor = LongMDYDateExtractor()

        assertEquals(LocalDateTimeBuilder.of(2023, 1, 12), extractor.find("Jan 12, 2023"))
        assertEquals(LocalDateTimeBuilder.of(1993, 1, 1), extractor.find("Jan 1st, 93"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 4), extractor.find("April 4th, 1993"))
        assertEquals(LocalDateTimeBuilder.of(1993, 12, 15), extractor.find("December 15, 1993"))
        assertEquals(LocalDateTimeBuilder.of(2000, 3, 3), extractor.find("March 3rd, 2000 "))
        assertEquals(LocalDateTimeBuilder.of(2022, 1, 22), extractor.find("January 22nd 2022"))

        val expectedNonMatches = listOf(
            "22-03",
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `retrieves date from in long form of day month year`() {
        val extractor = LongDMYDateExtractor()

        assertEquals(LocalDateTimeBuilder.of(2023, 1, 12), extractor.find("12th of January 2023"))
        assertEquals(LocalDateTimeBuilder.of(1993, 1, 1), extractor.find("1 January 93"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 4), extractor.find("4th April, 1993"))
        assertEquals(LocalDateTimeBuilder.of(1993, 12, 15), extractor.find("15th of December, 1993"))
        assertEquals(LocalDateTimeBuilder.of(2000, 3, 3), extractor.find("3 Mar, 00 "))
        assertEquals(LocalDateTimeBuilder.of(2022, 1, 22), extractor.find("22nd of Jan 2022"))

        val expectedNonMatches = listOf(
            "22-03",
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

    @Test
    fun `fun retrieves dates with month year format`() {
        val extractor = MYDateExtractor()

        assertEquals(LocalDateTimeBuilder.of(2022, 4, 15), extractor.find("4-2022"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("4-1993"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("4.1993"))
        assertEquals(LocalDateTimeBuilder.of(1993, 4, 15), extractor.find("4/1993"))
        assertEquals(LocalDateTimeBuilder.of(2022, 12, 15), extractor.find("12-2022"))
        assertEquals(LocalDateTimeBuilder.of(2022, 4, 15), extractor.find("04-2022"))

        val expectedNonMatches = listOf(
            "22-03",
            "2022041",
            "202204",
            "2A02204011"
        )

        expectedNonMatches.forEach {
            assertNull(extractor.find(it))
        }
    }

}