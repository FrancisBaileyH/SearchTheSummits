package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.configuration.ClientConfiguration
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.apache.tomcat.util.buf.HexUtils
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.core.env.Environment
import java.net.URL

class PageCrawlerServiceTest {

    private val url = URL("https://francisbailey.com")

    private val environment = mock<Environment>()

    private val regionConfig = mock<RegionConfig>()

    private val clientConfiguration = ClientConfiguration(environment, regionConfig)

    @Test
    fun `returns html page as text when fetched successfully`() {
        val content = Jsoup.parse("<html>Test</html>")
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            respond(
                content = content.html(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8")
            )
        }))

        assertEquals(content.html(), service.getHtmlDocument(url).html())
    }

    @Test
    fun `relative urls resolve correctly`() {
        val url = URL("https://test.com/forums/?test=3")

        val html = """
            <html>
                <body>
                    <a href="viewforum.php?f=5">Test</a>
                </body>
            </html>
        """

        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { _ ->
            respond(
                content = html,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        val document = service.getHtmlDocument(url)

        val links = document.getLinks()

        assertEquals(1, links.size)
        assertEquals("https://test.com/forums/viewforum.php?f=5", links.first())
    }

    @Test
    fun `relative urls with parent reference resolve correctly`() {
        val url = URL("https://test.com/OtherScrambles/index.html")

        val html = """
            <html>
                <body>
                    <a href="../NugaraScrambles/Anderson/AndersonPeak.html">Test</a>
                </body>
            </html>
        """

        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { _ ->
            respond(
                content = html,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        val document = service.getHtmlDocument(url)

        val links = document.getLinks()

        assertEquals(1, links.size)
        assertEquals("https://test.com/NugaraScrambles/Anderson/AndersonPeak.html", links.first())
    }

    @Test
    fun `fallsback to ISO_8859_1 charset on MalformedInputException`() {
        val invalidUTF8ByteSequence: ByteArray = HexUtils.fromHexString("ff")

        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { _ ->
            respond(
                content = invalidUTF8ByteSequence,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        val text = service.getHtmlDocument(URL("http://test.com"))
        assertEquals("ÿ", text.text())
    }

    @Test
    fun `throws redirected page exception with location on redirect errors`() {
        val redirectCodes = setOf(
            HttpStatusCode.MovedPermanently,
            HttpStatusCode.Found,
            HttpStatusCode.TemporaryRedirect,
            HttpStatusCode.PermanentRedirect,
            HttpStatusCode.SeeOther,
        )
        val location = "https://some-url.com"

        redirectCodes.forEach { code ->
            val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
                respond(
                    content = "<html>Test</html>",
                    status = code,
                    headers = headersOf(HttpHeaders.Location, location)
                )
            }))

            assertThrows<RedirectedPageException> { service.getHtmlDocument(url) }.also {
                assertEquals(location, it.location)
            }
        }
    }

    @Test
    fun `throws UnparsableContentException when htmlParser call fails`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respond(
                content = "<html>Test</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })) {
           throw RuntimeException("Couldn't Parse Test")
        }

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws UnparsableContentException when parsing fails`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respond(
                content = byteArrayOf(Byte.MAX_VALUE.plus(1).toByte()), // Create a malformed ByteArray
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        })) { throw RuntimeException("Failed to Parse") }

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws NonRetryablePageException when status is between 400 and 499 except 429`() {
        (400..428).plus(430..499).forEach { errorCode ->
            val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Test Client Error"))
            }))

            assertThrows<NonRetryablePageException> { service.getHtmlDocument(url) }
        }
    }

    @Test
    fun `throws RetryablePageException on ServerResponseException encounter`() {
        (500..599).forEach { errorCode ->
            val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Server Error"))
            }).config {
                install(HttpRequestRetry) {
                    noRetry() // disable for 50X errors to speed up tests
                }
            })

            assertThrows<RetryablePageException> { service.getHtmlDocument(url) }
        }
    }

    @Test
    fun `throws RetryablePageException on 429 error code`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respondError(HttpStatusCode(429, "Server Error"))
        }))

        assertThrows<RetryablePageException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws UnparsablePageException when content type is not text html`() {
        val content = Jsoup.parse("<html>Test</html>")
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            respond(
                content = content.html(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }))

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }
}