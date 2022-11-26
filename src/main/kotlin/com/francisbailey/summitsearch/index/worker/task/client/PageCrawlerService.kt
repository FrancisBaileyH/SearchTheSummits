package com.francisbailey.summitsearch.index.worker.task.client

import io.ktor.client.*
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
     * @TODO need to handle 302 redirects and 404s at some point
     */
    fun getHtmlContentAsString(pageUrl: URL) = runBlocking {
        log.info { "Fetching HTML content from: $pageUrl" }

        val response = httpClient.get(pageUrl)
        check(response.status == HttpStatusCode.OK) {
            "Unexpected response when fetching from: $pageUrl"
        }

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

}

class InvalidContentTypeException(message: String): Exception(message)
class UnparsableContentException(message: String): Exception(message)