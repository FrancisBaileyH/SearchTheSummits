package com.francisbailey.summitsearch.index.worker.extractor.strategy

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DefaultContentExtractorStrategyTest {

    private val extractor = DefaultContentExtractorStrategy()

    @Test
    fun `extracts expected values from document`() {
        val html = """
            <html>
                <head>
                    <title>Test Page!</title>
                    <meta name="description" content="this is a test">
                </head>
                <body>
                    <p>Test content.</p>
                    <div>Hello world</div>
                    <footer><h1>Footer</h1></footer>
                </body>
            </html>
        """

        val result = extractor.extract(Jsoup.parse(html))
        Assertions.assertEquals("Test content. Hello world", result.rawText)
        Assertions.assertEquals("Test content.", result.semanticText)
        Assertions.assertEquals("this is a test", result.description)
        Assertions.assertEquals("Test Page!", result.title)
    }

    @Test
    fun `excluded tags are processed as expected`() {
        val htmlWithExcludedTags = """
            <html>
                <body>
                    <header><p>Here is header</p></header>
                    <nav><p>Some Nav Thing</p></nav>
                    <p>Test content.</p>
                    <ul>
                      <li><a>Bad Content</a></li>
                    </ul>
                    <footer><h1>Footer</h1></footer>
                </body>
            </html>
        """.trimIndent()

        val result = extractor.extract(Jsoup.parse(htmlWithExcludedTags))

        Assertions.assertEquals("Test content.", result.rawText)
        Assertions.assertEquals("Test content.", result.semanticText)
    }
}