package com.francisbailey.summitsearch.index.worker.client

import com.francisbailey.summitsearch.index.worker.configuration.CrawlerConfiguration
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.charsets.Charsets
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL

/**
 * @TODO handle redirects
 */
@Service
class PageCrawlerService(
    private val httpClient: HttpClient,
    private val crawlerConfiguration: CrawlerConfiguration,
    private val htmlParser: (String) -> Document
) {
    private val log = KotlinLogging.logger { }

    @Autowired
    constructor(httpClient: HttpClient, crawlerConfiguration: CrawlerConfiguration): this(
        httpClient,
        crawlerConfiguration,
        { Jsoup.parse(it) }
    )

    /**
     * With expectSuccess enabled on client, we will see ServerResponseException and ClientResponseExceptions
     * being thrown here
     */
    fun getHtmlDocument(pageUrl: URL): Document = runBlocking {
        log.info { "Fetching HTML content from: $pageUrl" }

        val response = getPage(pageUrl)
        val charset = crawlerConfiguration.charsetOverride[pageUrl.host] ?: response.charset() ?: Charsets.UTF_8

        try {
            htmlParser(response.bodyAsText(charset)).also {
                it.setBaseUri(pageUrl.toString().substringBeforeLast("/")) // need to fetch relative href links
                log.info { "Successfully retrieved HTML content from: $pageUrl" }
            }
        } catch (e: Exception) {
            throw UnparsableContentException("Unable to parse content as text from: $pageUrl. Reason: ${e.message}", e)
        }
    }

    private suspend fun getPage(pageUrl: URL): HttpResponse {
        return try {
            httpClient.get(pageUrl)
        } catch (e: Exception) {
            when (e) {
                is ClientRequestException,
                is RedirectResponseException -> {
                    val exception = e as ResponseException
                    if (exception.response.status == HttpStatusCode.TooManyRequests) {
                        throw RetryablePageException(e.localizedMessage, e)
                    }
                    throw PermanentNonRetryablePageException("Found non retryable exception", e)
                }
                is SendCountExceedException,
                is ServerResponseException -> {
                    throw RetryablePageException(e.localizedMessage, e)
                }
                else -> throw e
            }
        }
    }
}

fun Element.getLinks(): List<String> {
    return this.select("a[href]").map { it.attr("abs:href") }
}

open class RetryablePageException(message: String, e: Exception): RuntimeException(message, e)
open class TemporaryNonRetryablePageException(message: String, e: Exception): RetryablePageException(message, e)
open class PermanentNonRetryablePageException(message: String, e: Exception): RuntimeException(message, e)

class UnparsableContentException(message: String, e: Exception): TemporaryNonRetryablePageException(message, e)