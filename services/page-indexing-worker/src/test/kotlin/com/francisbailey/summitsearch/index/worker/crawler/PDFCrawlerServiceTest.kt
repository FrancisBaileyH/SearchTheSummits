package com.francisbailey.summitsearch.index.worker.crawler

import com.francisbailey.summitsearch.index.worker.extension.ExperimentalContentType
import io.ktor.client.statement.*
import io.ktor.http.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration

class PDFCrawlerServiceTest {

    private val pdfLoader = mock<(ByteArray) -> PDDocument>()

    private val httpCrawlerClient = mock<HttpCrawlerClient>()

    private val timeout = Duration.ofSeconds(5)

    private val pdfCrawler = PDFCrawlerService(httpCrawlerClient, pdfLoader, timeout)

    private val url = URL("https://francisbaileyh.com")

    private val document = mock<PDDocument>()

    @Test
    fun `returns PDDocument when url loads correctly`() {
        val data = ByteArray(1)
        val response = getResponse(url, data, ContentType.Application.Pdf)

        whenever(httpCrawlerClient.get<PDDocument>(any(), any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        whenever(pdfLoader(any())).thenReturn(document)

        val result = pdfCrawler.get(url)

        assertEquals(document, result)
        verify(pdfLoader).invoke(data)
        verify(httpCrawlerClient).get(eq(url), any(), any<(HttpResponse) -> Document>(), eq(timeout))
    }

    @Test
    fun `returns PDDocument when x-pdf type returned`() {
        val data = ByteArray(1)
        val response = getResponse(url, data, ExperimentalContentType.xPdf)

        whenever(httpCrawlerClient.get<PDDocument>(any(), any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        whenever(pdfLoader(any())).thenReturn(document)

        val result = pdfCrawler.get(url)

        assertEquals(document, result)
        verify(pdfLoader).invoke(data)
        verify(httpCrawlerClient).get(eq(url), any(), any<(HttpResponse) -> Document>(), eq(timeout))
    }

    @Test
    fun `throws exception when content type is not PDF`() {
        val data = ByteArray(1)
        val response = getResponse(url, data, ContentType.Application.Json)

        whenever(httpCrawlerClient.get<PDDocument>(any(), any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> Document>(2).invoke(response)
        }

        assertThrows<UnparsableEntityException> { pdfCrawler.get(url) }
    }

}