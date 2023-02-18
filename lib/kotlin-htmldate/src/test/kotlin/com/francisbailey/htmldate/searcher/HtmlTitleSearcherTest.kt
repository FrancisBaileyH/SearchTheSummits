package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.extension.LocalDateTimeBuilder
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class HtmlTitleSearcherTest {

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlTitleSearcher(parser)

    @Test
    fun `searches any h1 title elements for date`() {
        val date = LocalDateTimeBuilder.of(2022)
        val html = """
            <html>
                <body>
                    <h1>There is a date here</h1>
                    <p>Some where here too</p>
                    <h1>One more date</h1>
                </body>
            </html>
        """

        val document = Jsoup.parse(html)

        whenever(parser.parse("One more date")).thenReturn(date)

        val result = searcher.getDateValue(document)

        verify(parser).parse("There is a date here")
        verify(parser).parse("One more date")

        assertEquals(date, result)
    }
}