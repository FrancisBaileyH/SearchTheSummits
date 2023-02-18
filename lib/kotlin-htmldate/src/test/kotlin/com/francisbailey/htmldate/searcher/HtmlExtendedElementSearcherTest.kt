package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HtmlExtendedElementSearcherTest {

    class TestSearcher(
        configuration: HtmlDateSearchConfiguration,
        parser: HtmlDateParser
    ) : HtmlExtendedElementSearcher(configuration, parser) {

        override fun selectExtendedElements(document: Document): List<Element> {
            return document.select("p")
        }

    }

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
        on(mock.maxElementScan).thenReturn(150)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = TestSearcher(configuration, parser)

    private val date = LocalDateTimeBuilder.of(2023)


    @Test
    fun `gets date from first 48 characters of text`() {
        whenever(parser.parse("Jan 12, 2022")).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <p>Jan 12, 2022 abc</p>
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }

    @Test
    fun `gets date from first 48 characters of title attribute`() {
        whenever(parser.parse("123456789")).thenReturn(date)

        val document = Jsoup.parse("""
            <html>
                <body>
                    <p title="123456789">abc abc</p>
                </body>
            </html>
        """.trimIndent())

        assertEquals(date, searcher.getDateValue(document))
    }


}