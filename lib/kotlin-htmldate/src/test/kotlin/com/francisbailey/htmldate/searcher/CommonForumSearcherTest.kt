package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.LocalDateTime


class CommonForumSearcherTest {

    private val parser = mock<HtmlDateParser>()

    @Test
    fun `extracts date from post details`() {
        val date = LocalDateTime.now()
        whenever(parser.parse(any())).thenReturn(date)

        val html = Jsoup.parse(File("src/test/resources/dated-pages/phpbb.html"))
        val searcher = CommonForumSearcher(parser)

        assertEquals(date, searcher.getDateValue(html))
        verify(parser).parse("Mon Feb 13, 2023 10:02 pm")
    }

}