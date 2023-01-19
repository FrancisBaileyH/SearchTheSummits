package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.isRedirect
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URL

@Service
class HttpCrawlerClient(
    private val httpClient: HttpClient
) {
    private val log = KotlinLogging.logger { }

    fun <T> getContent(pageUrl: URL, responseValidationInterceptor: (HttpResponse) -> Unit, transformer: (HttpResponse) -> T): T {
        log.info { "Fetching HTML content from: $pageUrl" }
        val response = getContent(pageUrl, responseValidationInterceptor)

        return try {
            runBlocking {
                transformer(response.body())
            }
        } catch (e: Exception) {
            throw UnparsableEntityException("Unable to parse content as text from: $pageUrl. Reason: ${e.message}")
        }
    }

    private fun getContent(pageUrl: URL, responseValidationInterceptor: (HttpResponse) -> Unit): HttpResponse {
        val response = runBlocking { httpClient.get(pageUrl) }

        if (response.status.value < 300) {
            responseValidationInterceptor(response)
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
open class RedirectedEntityException(val location: String?, message: String): RuntimeException(message)