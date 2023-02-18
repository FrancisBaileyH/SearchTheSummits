package com.francisbailey.htmldate.searcher

import com.francisbailey.htmldate.HtmlDateParser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class HtmlExtendedAbbrSearcherTest {

    private val configuration = mock<HtmlDateSearchConfiguration> {
        on(mock.useOriginalDate).thenReturn(true)
    }

    private val parser = mock<HtmlDateParser>()

    private val searcher = HtmlExtendedAbbrSearcher(configuration, parser)


    @Test
    fun `returns abbr elements`() {

        val document = Jsoup.parse("""
            <html>
                <body>
                    <h1>A title</h1
                    <p>Some text</p>
                    <abbr>test</abbr>
                    <div>Some other text</div>
                    <span>Here too</span>
                    <ul>
                        <li>May be date here</li>
                    </ul>   
                </body>
            </html>"""
        )

        val elements = searcher.selectExtendedElements(document)

        Assertions.assertEquals(1, elements.count { it.tag().name == "abbr" })
        Assertions.assertEquals(1, elements.size)

    }
}