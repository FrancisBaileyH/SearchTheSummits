package com.francisbailey.summitsearch.index.worker.client

import com.francisbailey.summitsearch.index.worker.configuration.ClientConfiguration
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.springframework.core.env.Environment
import java.net.URL

class PageCrawlerServiceTest {

    private val url = URL("https://francisbailey.com")

    private val environment = mock<Environment>()

    private val clientConfiguration = ClientConfiguration(environment)

    @Test
    fun `returns html page as text when fetched successfully`() {
        val content = Jsoup.parse("<html>Test</html>")
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            respond(
                content = content.html(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        assertEquals(content.html(), service.getHtmlDocument(url).html())
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

        assertThrows<UnparsableContentException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws UnparsableContentException when bodyAsText call fails`() {
        val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
            respond(
                content = byteArrayOf(Byte.MAX_VALUE.plus(1).toByte()), // Create a malformed ByteArray
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        assertThrows<UnparsableContentException> { service.getHtmlDocument(url) }
    }

    @Test
    fun `throws PermanentNonRetryablePageException on Client and Retry exception encounters`() {
        (300..428).plus(430..499).forEach { errorCode ->
            println(errorCode)
            val service = PageCrawlerService(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Test Client Error"))
            }))

            assertThrows<PermanentNonRetryablePageException> { service.getHtmlDocument(url) }
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
}