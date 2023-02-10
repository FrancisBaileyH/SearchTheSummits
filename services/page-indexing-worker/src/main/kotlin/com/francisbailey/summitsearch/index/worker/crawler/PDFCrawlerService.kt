package com.francisbailey.summitsearch.index.worker.crawler

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL

@Service
class PDFCrawlerService(
    private val httpCrawlerClient: HttpCrawlerClient,
    private val pdfLoader: (ByteArray) -> PDDocument
) {
    @Autowired
    constructor(
        httpCrawlerClient: HttpCrawlerClient
    ): this(httpCrawlerClient, { PDDocument.load(it) })

    private val validator: (HttpResponse) -> Unit = {
        val isPDF = it.contentType()?.match(ContentType.Application.Pdf) ?: false

        if (!isPDF) {
            throw UnparsableEntityException("Content type is not PDF: ${it.contentType()}")
        }
    }

    private val transformer: (HttpResponse) -> PDDocument = {
        val pdfData = runBlocking { it.body<ByteArray>() }
        pdfLoader(pdfData)
    }

    fun get(url: URL): PDDocument {
        return httpCrawlerClient.get(url, contentValidationInterceptor = validator, transformer = transformer)
    }

}