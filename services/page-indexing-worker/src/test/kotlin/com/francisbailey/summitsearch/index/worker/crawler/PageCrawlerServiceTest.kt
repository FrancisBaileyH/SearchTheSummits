package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.configuration.ClientConfiguration
import com.francisbailey.summitsearch.index.worker.configuration.CrawlerConfiguration
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.core.env.Environment
import java.net.URL

class PageCrawlerServiceTest {

    private val url = URL("https://francisbailey.com")

    private val environment = mock<Environment>()

    private val crawlerConfiguration = mock<CrawlerConfiguration> {
        on(mock.charsetOverride).thenReturn(hashMapOf())
    }

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
        }), crawlerConfiguration)

        assertEquals(content.html(), service.getHtmlDocument(url).html())
    }

    @Test
    fun `sets base URI correctly`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { _ ->
            respond(
                content = "<html>Test</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }), crawlerConfiguration)

        val expectations = mapOf(
            "https://francisbailey.com/test#abc" to "https://francisbailey.com",
            "https://francisbailey.com/test/test123/test.html" to "https://francisbailey.com/test/test123",
            "https://francisbailey.com/test/test123?query=x#fragment" to "https://francisbailey.com/test",
            "https://francisbailey.com" to "https://francisbailey.com",
            "https://francisbailey.com/" to "https://francisbailey.com"
        )

        expectations.forEach {
            val document = service.getHtmlDocument(URL(it.key))
            assertEquals(it.value, document.baseUri())
        }
    }

    /** @TODO FIX NPE WHEN WE HAVE TIME
    @Test
    fun `uses charset override if one is present`() {
        whenever(crawlerConfiguration.charsetOverride).thenReturn(hashMapOf(url.host to Charsets.ISO_8859_1))
        val content = Jsoup.parse("<html>Test</html>")
        val response = mock<HttpResponse>()
//        val client = mock<HttpClient>()
//
//        response.stub {
//            onBlocking { bodyAsText(Charsets.ISO_8859_1) }.doReturn(content.html())
//            onBlocking { bodyAsText() }.doReturn(content.html())
//        }
//        client.stub {
//            onBlocking { get(any<String>()) }.doReturn(response)
//        }

        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            mockResponse(response)
        }), crawlerConfiguration)

        assertEquals(content.html(), service.getHtmlDocument(url).html())
        verifyBlocking(response) { bodyAsText(Charsets.ISO_8859_1) }
    }
  */
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
            }), crawlerConfiguration)

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
        }), crawlerConfiguration) {
           throw RuntimeException("Couldn't Parse Test")
        }

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws UnparsableContentException when bodyAsText call fails`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respond(
                content = byteArrayOf(Byte.MAX_VALUE.plus(1).toByte()), // Create a malformed ByteArray
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }), crawlerConfiguration)

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws NonRetryablePageException when status is between 400 and 499 except 429`() {
        (400..428).plus(430..499).forEach { errorCode ->
            val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Test Client Error"))
            }), crawlerConfiguration)

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
            }, crawlerConfiguration)

            assertThrows<RetryablePageException> { service.getHtmlDocument(url) }
        }
    }

    @Test
    fun `throws RetryablePageException on 429 error code`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respondError(HttpStatusCode(429, "Server Error"))
        }), crawlerConfiguration)

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
        }), crawlerConfiguration)

        assertThrows<UnparsablePageException> { service.getHtmlDocument(url) }
    }
}