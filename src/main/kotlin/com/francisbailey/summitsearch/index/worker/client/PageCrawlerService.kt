package com.francisbailey.summitsearch.index.worker.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URL

@Service
class PageCrawlerService(
    private val httpClient: HttpClient
) {
    private val log = KotlinLogging.logger { }

    /**
     * With expectSuccess enabled on client, we will see ServerResponseException and ClientResponseExceptions
     * being thrown here
     */
    fun getHtmlContentAsString(pageUrl: URL): String = runBlocking {
        log.info { "Fetching HTML content from: $pageUrl" }

        val response = getPage(pageUrl)

        response.contentType()?.fileExtensions()
        if (response.contentType() != ContentType.Text.Html) {
            throw InvalidContentTypeException("Unexpected content type: ${response.contentType()} from $pageUrl")
        }

        try {
            response.bodyAsText().also {
                log.info { "Successfully retrieved HTML content from: $pageUrl" }
            }
        } catch (e: Exception) {
            throw UnparsableContentException("Unable to parse content as text from: $pageUrl. Reason: ${e.message}")
        }
    }

    private suspend fun getPage(pageUrl: URL): HttpResponse {
        return try {
            httpClient.get(pageUrl)
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException,
                is RedirectResponseException -> {
                    throw PermanentNonRetryablePageException(e.localizedMessage)
                }
                is ServerResponseException -> {
                    throw RetryablePageException(e.localizedMessage)
                }
                else -> throw e
            }
        }
    }
}

open class RetryablePageException(message: String): RuntimeException(message)
open class TemporaryNonRetryablePageException(message: String): RetryablePageException(message)
open class PermanentNonRetryablePageException(message: String): RuntimeException(message)

class InvalidContentTypeException(message: String): PermanentNonRetryablePageException(message)
class UnparsableContentException(message: String): TemporaryNonRetryablePageException(message)