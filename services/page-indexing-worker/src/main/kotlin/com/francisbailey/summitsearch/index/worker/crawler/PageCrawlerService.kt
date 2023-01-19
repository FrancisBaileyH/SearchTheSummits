package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.bodyAsTextWithFallback
import com.francisbailey.summitsearch.index.worker.extension.isRedirect
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL


@Service
class PageCrawlerService(
    private val httpClient: HttpClient,
    private val htmlParser: (String) -> Document
) {
    private val log = KotlinLogging.logger { }

    @Autowired
    constructor(httpClient: HttpClient): this(
        httpClient,
        { Jsoup.parse(it) }
    )

    fun getHtmlDocument(pageUrl: URL): Document = runBlocking {
        log.info { "Fetching HTML content from: $pageUrl" }
        val response = getPage(pageUrl)

        try {
            val responseText = runBlocking { response.bodyAsTextWithFallback(FALLBACK_CHARSET) }
            htmlParser(responseText).also {
                log.info { "Successfully retrieved HTML content from: $pageUrl" }
                if (it.baseUri().isNullOrBlank()) {
                    it.setBaseUri(pageUrl.toString()) // needed to fetch relative href links
                }
            }
        } catch (e: Exception) {
            throw UnparsablePageException("Unable to parse content as text from: $pageUrl. Reason: ${e.message}")
        }
    }

    private fun getPage(pageUrl: URL): HttpResponse {
        val response = runBlocking { httpClient.get(pageUrl) }
        val contentType = response.contentType()

        if (response.status.value < 300) {
            if (contentType == null || !contentType.match(ContentType.Text.Html)) {
                throw UnparsablePageException("Can't process non HTML page from: $pageUrl")
            }

            return response
        }

        if (response.status.isRedirect()) {
            throw RedirectedPageException(location = response.headers[HttpHeaders.Location], "Found redirect on: $pageUrl")
        }

        when(response.status.value) {
            429 -> throw RetryablePageException("Throttled on $pageUrl")
            in 400..499 -> throw NonRetryablePageException("Status: ${response.status} when retrieving: $pageUrl")
            in 500 .. 599 -> throw RetryablePageException("Status: ${response.status} when retrieving: $pageUrl")
            else -> throw NonRetryablePageException("Unknown status: ${response.status} when retrieving: $pageUrl")
        }
    }

    companion object {
        val FALLBACK_CHARSET = Charsets.ISO_8859_1
    }
}

open class NonRetryablePageException(message: String): RuntimeException(message)
open class RetryablePageException(message: String): RuntimeException(message)
open class UnparsablePageException(message: String): NonRetryablePageException(message)
open class RedirectedPageException(val location: String?, message: String): RuntimeException(message)