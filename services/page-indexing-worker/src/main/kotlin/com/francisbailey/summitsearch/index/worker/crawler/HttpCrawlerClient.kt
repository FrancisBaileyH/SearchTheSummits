package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.isRedirect
import io.ktor.client.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Duration

@Service
class HttpCrawlerClient(
    private val httpClient: HttpClient
) {
    private val log = KotlinLogging.logger { }

    fun <T> get(pageUrl: URL, contentValidationInterceptor: (HttpResponse) -> Unit, transformer: (HttpResponse) -> T): T {
        return get(pageUrl, contentValidationInterceptor, transformer, null)
    }

    fun <T> get(
        pageUrl: URL,
        contentValidationInterceptor: (HttpResponse) -> Unit,
        transformer: (HttpResponse) -> T,
        timeout: Duration?
    ): T {
        log.info { "Fetching content from: $pageUrl" }
        val response = get(pageUrl, contentValidationInterceptor, timeout)

        return try {
            transformer(response)
        } catch (e: Exception) {
            log.debug(e) { "Failed to fetch content from: $pageUrl" }
            throw UnparsableEntityException("Unable to parse content from: $pageUrl. Reason: ${e.message}.")
        }
    }

    private fun get(pageUrl: URL, contentValidationInterceptor: (HttpResponse) -> Unit, requestTimeout: Duration?): HttpResponse {
        val response = try {
            runBlocking { httpClient.get(pageUrl) {
                requestTimeout?.let {
                    timeout {
                        requestTimeoutMillis = requestTimeout.toMillis()
                    }
                }
            }}
        } catch (e: Exception) {
            when (e) {
                is HttpRequestTimeoutException,
                is ConnectTimeoutException,
                is SocketTimeoutException -> {
                    throw RetryableEntityException("Request timeout on: $pageUrl. ${e.localizedMessage}")
                }
                else -> throw e
            }
        }

        if (response.status.value < 300) {
            contentValidationInterceptor(response)
            return response
        }

        if (response.status.isRedirect()) {
            throw RedirectedEntityException(
                location = response.headers[HttpHeaders.Location],
                "Found redirect on: $pageUrl"
            )
        }

        when (response.status.value) {
            429 -> throw RetryableEntityException("Throttled on $pageUrl")
            in 400..499 -> throw NonRetryableEntityException("Status: ${response.status} when retrieving: $pageUrl")
            in 500..599 -> throw RetryableEntityException("Status: ${response.status} when retrieving: $pageUrl")
            else -> throw NonRetryableEntityException("Unknown status: ${response.status} when retrieving: $pageUrl")
        }
    }
}

open class NonRetryableEntityException(message: String): RuntimeException(message)
open class RetryableEntityException(message: String): RuntimeException(message)
open class UnparsableEntityException(message: String): NonRetryableEntityException(message)
open class RedirectedEntityException(val location: String?, message: String): NonRetryableEntityException(message)
open class UnsupportedEntityException(val contentType: ContentType?, message: String): NonRetryableEntityException(message)