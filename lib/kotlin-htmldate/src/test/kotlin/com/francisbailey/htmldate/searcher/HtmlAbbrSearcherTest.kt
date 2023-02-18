package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HtmlAbbrSearcherTest {

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
        on(mock.maxElementScan).thenReturn(150)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlAbbrSearcher(configuration, parser)

    private val date = LocalDateTimeBuilder.of(2023)


    @Test
    fun `returns date if date-utime attribute is present`() {
        whenever(parser.parse(any())).thenReturn(date)
        whenever(configuration.useOriginalDate).thenReturn(false)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <abbr data-utime="1234567"></abbr>
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }

    @Test
    fun `returns date if date-utime attribute is not numeric`() {
        whenever(parser.parse(any())).thenReturn(date)
        whenever(configuration.useOriginalDate).thenReturn(false)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <abbr data-utime="abc123"></abbr>
                </body>
            </html>
        """.trimIndent())

        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `returns date if css date class attribute and title attribute is present`() {
        whenever(parser.parse(any())).thenReturn(date)
        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.CLASS_ATTRIBUTE_KEYS.forEach {
            var html = "<html><body>"
            html += "<abbr class=\"$it\" title=\"123456\" />"
            html += "</body></html>"
            val document = Jsoup.parse(html)

            assertEquals(date, searcher.getDateValue(document))
        }
    }

    @Test
    fun `returns date if css date class attribute and text value is present`() {
        whenever(parser.parse(any())).thenReturn(date)
        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.CLASS_ATTRIBUTE_KEYS.forEach {
            var html = "<html><body>"
            html += "<abbr class=\"$it\">12345678910</abbr>"
            html += "</body></html>"
            val document = Jsoup.parse(html)

            assertEquals(date, searcher.getDateValue(document))
        }
    }

    @Test
    fun `does not return date if css date class attribute and text value is less than 10`() {
        whenever(parser.parse(any())).thenReturn(date)
        whenever(configuration.useOriginalDate).thenReturn(false)

        HtmlDateAttributes.CLASS_ATTRIBUTE_KEYS.forEach {
            var html = "<html><body>"
            html += "<abbr class=\"$it\">123456789</abbr>"
            html += "</body></html>"
            val document = Jsoup.parse(html)

            assertNull(searcher.getDateValue(document))
        }
    }

    @Test
    fun `returns oldest date from numerous time elements if use original date is true`() {
        var counter = 0L

        whenever(parser.parse(any())).thenAnswer {
            counter += 1
            date.minusDays(counter)
        }

        // title should get overriden by text
        val document = Jsoup.parse("""
            <html>
                <body>
                    <abbr data-utime="1234567" />
                    <abbr class="published">12345678910</abbr>
                    <abbr class="published" title="1234567">12345678910</abbr>
                </body>
            </html>
        """.trimIndent())

        val result = searcher.getDateValue(document)

        assertEquals(date.minusDays(counter), result)
        assertEquals(4, counter)
    }

    @Test
    fun `returns newest date from numerous time elements if use original date is false`() {
        var counter = 0L

        whenever(parser.parse(any())).thenAnswer {
            counter += 1
            date.plusDays(counter)
        }
        whenever(configuration.useOriginalDate).thenReturn(false)

        // title should get overriden by text
        val document = Jsoup.parse("""
            <html>
                <body>
                    <abbr data-utime="1234567" />
                    <abbr class="published">12345678910</abbr>
                    <abbr class="published" title="1234567">12345678910</abbr>
                </body>
            </html>
        """.trimIndent())

        val result = searcher.getDateValue(document)

        assertEquals(date.plusDays(counter), result)
        assertEquals(4, counter)
    }
}