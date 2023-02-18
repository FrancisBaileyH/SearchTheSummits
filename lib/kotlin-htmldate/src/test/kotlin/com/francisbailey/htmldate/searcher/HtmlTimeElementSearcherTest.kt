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

class HtmlTimeElementSearcherTest {

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
        on(mock.maxElementScan).thenReturn(150)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlTimeElementSearcher(configuration, parser)

    private val date = LocalDateTimeBuilder.of(2023)

    @Test
    fun `skips if time content size is less than 6`() {
        val document = Jsoup.parse("""
            <html>
                <body>
                    <time>123456</time>
                </body>
            </html>
        """.trimIndent())

        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `skips if datetime attribute is less than 6`() {
        val document = Jsoup.parse("""
            <html>
                <body>
                    <time datetime="123456"></time>
                </body>
            </html>
        """.trimIndent())

        assertNull(searcher.getDateValue(document))
    }

    @Test
    fun `returns date if datetime is date and pubdate is present`() {
        whenever(parser.parse(any())).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time datetime="1234567" pubdate="pubdate" />
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }

    @Test
    fun `returns date if datetime is date and entry-date class is present`() {
        whenever(parser.parse(any())).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time datetime="1234567" pubdate="pubdate" class="entry-date" />
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }

    @Test
    fun `returns date if datetime is date and entry-time class is present`() {
        whenever(parser.parse(any())).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time datetime="1234567" pubdate="pubdate" class="entry-time" />
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }

    @Test
    fun `returns date if time text is present is present`() {
        whenever(parser.parse(any())).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time>1234567</time>
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }


    @Test
    fun `returns oldest date from numerous time elements if use original date is true`() {
        var counter = 0L

        whenever(parser.parse(any())).thenAnswer {
            counter += 1
            date.minusDays(counter)
        }

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time>1234567</time>
                    <time>1234567</time>
                    <time>1234567</time>
                </body>
            </html>
        """.trimIndent())

        val result = searcher.getDateValue(document)

        assertEquals(date.minusDays(counter), result)
        assertEquals(3 , counter)
    }

    @Test
    fun `returns newest date from numerous time elements if use original date is false`() {
        var counter = 0L

        whenever(parser.parse(any())).thenAnswer {
            counter += 1
            date.plusDays(counter)
        }
        whenever(configuration.useOriginalDate).thenReturn(false)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <time>1234567</time>
                    <time>1234567</time>
                    <time>1234567</time>
                </body>
            </html>
        """.trimIndent())

        val result = searcher.getDateValue(document)

        assertEquals(date.plusDays(counter), result)
        assertEquals(3 , counter)
    }
}