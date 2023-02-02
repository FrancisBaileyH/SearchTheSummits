package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.getLinks
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class PageCrawlerServiceTest {

    private val httpCrawlerClient = mock<HttpCrawlerClient>()

    private val pageCrawler = PageCrawlerService(httpCrawlerClient)

    private val url = URL("https://francisbaileyh.com")


    @Test
    fun `returns html page as text when fetched successfully`() {
        val content = "<html>Test</html>"
        val response = getResponse(url, content, ContentType.Text.Html)

        whenever(httpCrawlerClient.get<Document>(any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        val html = Jsoup.parse(content).html()
        val result = pageCrawler.get(url)

        Assertions.assertEquals(html, result.html())
    }

    @Test
    fun `relative urls resolve correctly`() {
        val url = URL("https://test.com/forums/?test=3")
        val content = """
            <html>
                <body>
                    <a href="viewforum.php?f=5">Test</a>
                </body>
            </html>
        """

        val response = getResponse(url, content, ContentType.Text.Html)

        whenever(httpCrawlerClient.get<Document>(any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        val document = pageCrawler.get(url)

        val links = document.getLinks()

        Assertions.assertEquals(1, links.size)
        Assertions.assertEquals("https://test.com/forums/viewforum.php?f=5", links.first())
    }

    @Test
    fun `throws UnparsableEntityException when content validation fails`() {
        val content = "<html>Test</html>"
        val response = getResponse(url, content, ContentType.Image.PNG)

        whenever(httpCrawlerClient.get<Document>(any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        assertThrows<UnparsableEntityException> { pageCrawler.get(url) }
    }

}