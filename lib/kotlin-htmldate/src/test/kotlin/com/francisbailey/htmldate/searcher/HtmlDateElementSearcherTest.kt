package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class HtmlDateElementSearcherTest {

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlDateElementSearcher(configuration, parser)


    @Test
    fun `returns li p div and span elements with date class`() {
        HtmlDateAttributes.DATE_CLASS_INDICATORS.forEach {
            val document = Jsoup.parse("<html><body" +
                "<h1 class=\"$it\">A title</h1>" +
                    "<p class=\"$it\">Some text</p>" +
                    "<article class=\"$it\">test</article>" +
                    "<div class=\"$it\">Some other text</div>" +
                    "<span class=\"$it\">Here too</span>" +
                    "<ul>" +
                       "<li class=\"$it\">May be date here</li>" +
                    "</ul>   " +
                "</body></html>")

            val elements = searcher.selectExtendedElements(document)

            assertEquals(1, elements.count { it.tag().name == "p" })
            assertEquals(1, elements.count { it.tag().name == "div" })
            assertEquals(1, elements.count { it.tag().name == "li" })
            assertEquals(1, elements.count { it.tag().name == "span" })
            assertEquals(4, elements.size)
        }
    }

    @Test
    fun `returns li p div and span elements with date id`() {
        HtmlDateAttributes.DATE_ID_INDICATORS.forEach {
            val document = Jsoup.parse("<html><body" +
                "<h1 id=\"$it\">A title</h1>" +
                "<p id=\"$it\">Some text</p>" +
                "<article id=\"$it\">test</article>" +
                "<div id=\"$it\">Some other text</div>" +
                "<span id=\"$it\">Here too</span>" +
                "<ul>" +
                "<li id=\"$it\">May be date here</li>" +
                "</ul>   " +
                "</body></html>")

            val elements = searcher.selectExtendedElements(document)

            assertEquals(1, elements.count { it.tag().name == "p" })
            assertEquals(1, elements.count { it.tag().name == "div" })
            assertEquals(1, elements.count { it.tag().name == "li" })
            assertEquals(1, elements.count { it.tag().name == "span" })
            assertEquals(4, elements.size)
        }
    }
}