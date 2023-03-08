package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.configuration.ClientConfiguration
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.ktor.client.engine.mock.*
import io.ktor.client.network.sockets.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.core.env.Environment
import java.net.URL
import java.time.Duration

class HttpCrawlerClientTest {

    private val url = URL("https://francisbailey.com")

    private val environment = mock<Environment>()

    private val regionConfig = mock<RegionConfig>()

    private val clientConfiguration = ClientConfiguration(environment, regionConfig)

    private val validator = mock<(HttpResponse) -> Unit>()

    private val transformer = mock<(HttpResponse) -> String>()


    @Test
    fun `throws redirected entity exception with location on redirect errors`() {
        val redirectCodes = setOf(
            HttpStatusCode.MovedPermanently,
            HttpStatusCode.Found,
            HttpStatusCode.TemporaryRedirect,
            HttpStatusCode.PermanentRedirect,
            HttpStatusCode.SeeOther,
        )
        val location = "https://some-url.com"

        redirectCodes.forEach { code ->
            val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine {
                respond(
                    content = "<html>Test</html>",
                    status = code,
                    headers = headersOf(HttpHeaders.Location, location)
                )
            }))

            assertThrows<RedirectedEntityException> { service.get(url, validator, transformer) }.also {
                assertEquals(location, it.location)
            }
        }
    }

    @Test
    fun `throws UnparsableEntityException when transformer call fails`() {
        whenever(transformer.invoke(any())).thenThrow(RuntimeException("Failed to transform"))

        val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine {
            respond(
                content = "<html>Test</html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html")
            )
        }))

        assertThrows<UnparsableEntityException> { service.get(url, validator, transformer) }
    }

    @Test
    fun `throws NonRetryablePageException when status is between 400 and 499 except 429`() {
        (400..428).plus(430..499).forEach { errorCode ->
            val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Test Client Error"))
            }))

            assertThrows<NonRetryableEntityException> { service.get(url, validator, transformer) }
        }
    }

    @Test
    fun `throws RetryablePageException on ServerResponseException encounter`() {
        (500..599).forEach { errorCode ->
            val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine {
                respondError(HttpStatusCode(errorCode, "Server Error"))
            }).config {
                install(HttpRequestRetry) {
                    noRetry() // disable for 50X errors to speed up tests
                }
            })

            assertThrows<RetryableEntityException> { service.get(url, validator, transformer) }
        }
    }

    @Test
    fun `throws RetryablePageException on 429 error code`() {
        val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine {
            respondError(HttpStatusCode(429, "Server Error"))
        }))

        assertThrows<RetryableEntityException> { service.get(url, validator, transformer) }
    }

    @Test
    fun `throws exception when content type validation fails`() {
        whenever(validator.invoke(any())).thenThrow(RuntimeException("Some issue"))
        val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }))

        assertThrows<RuntimeException> { service.get(url, validator, transformer) }

        verify(validator).invoke(check {
            assertEquals(ContentType.Application.Rss, it.contentType())
        })
    }

    @Test
    fun `throws RetryableEntityException when timeout occurs`() {
        val execeptions = listOf(
            SocketTimeoutException("test"),
            ConnectTimeoutException("test", null),
            HttpRequestTimeoutException("test", null)
        )

        execeptions.forEach { exception ->
            val engine = MockEngine(MockEngineConfig().apply {
                addHandler {
                    throw exception
                }
            })

            val service = HttpCrawlerClient(clientConfiguration.httpClient(engine).config {
                install(HttpRequestRetry) {
                    noRetry() // disable for timeout errors to speed up tests
                }
            })

            assertThrows<RetryableEntityException> { service.get(url, validator, transformer) }
        }
    }

    @Test
    fun `transforms data and returns result when 200 ok and content validation succeeds`() {
        val expectedResult = "Test"

        whenever(transformer.invoke(any())).thenReturn(expectedResult)

        val service = HttpCrawlerClient(clientConfiguration.httpClient(MockEngine { request ->
            assertEquals(url.toURI(), request.url.toURI())
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
            )
        }))

        val result = service.get(url, validator, transformer)

        assertEquals(expectedResult, result)

        verify(validator).invoke(check {
            assertEquals(ContentType.Text.Html, it.contentType())
        })
        verify(transformer).invoke(check {
            assertEquals(ContentType.Text.Html, it.contentType())
        })
    }

    @Test
    fun `times out on custom duration if one is supplied`() {
        val engine = MockEngine(MockEngineConfig().apply {
            addHandler {
                withContext(Dispatchers.IO) {
                    Thread.sleep(100) // this is not ideal...
                }
                respondOk("test")
            }
        })

        val service = HttpCrawlerClient(clientConfiguration.httpClient(engine).config {
            install(HttpRequestRetry) {
                noRetry() // disable for timeout errors to speed up tests
            }
        })

        assertThrows<RetryableEntityException> { service.get(url, validator, transformer, Duration.ofMillis(25)) }
    }
}