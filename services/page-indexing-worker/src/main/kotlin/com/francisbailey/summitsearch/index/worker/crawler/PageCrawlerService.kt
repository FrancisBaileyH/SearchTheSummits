package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.bodyAsTextWithFallback
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
    private val httpCrawlerClient: HttpCrawlerClient,
    private val htmlParser: (String) -> Document
) {
    private val log = KotlinLogging.logger { }

    @Autowired
    constructor(httpCrawlerClient: HttpCrawlerClient): this(
        httpCrawlerClient,
        { Jsoup.parse(it) }
    )

    private val validator: (HttpResponse) -> Unit = {
        val isHtml = it.contentType()?.match(ContentType.Text.Html) ?: false
        if (!isHtml) {
            throw UnparsableEntityException("Can't process non HTML page from: ${it.request.url}")
        }
    }

    private val transformer: (HttpResponse) -> Document = { response ->
        val responseText = runBlocking { response.bodyAsTextWithFallback(FALLBACK_CHARSET) }
        htmlParser(responseText).also {
            log.info { "Successfully retrieved HTML content from: ${response.request.url}" }
            if (it.baseUri().isNullOrBlank()) {
                it.setBaseUri(response.request.url.toString()) // needed to fetch relative href links
            }
        }
    }

    fun get(pageUrl: URL): Document {
        return httpCrawlerClient.getContent(pageUrl,
            responseValidationInterceptor = validator,
            transformer = transformer
        )
    }

    companion object {
        val FALLBACK_CHARSET = Charsets.ISO_8859_1
    }
}