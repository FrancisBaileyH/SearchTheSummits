package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.PDFCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchPDFStep
import org.apache.pdfbox.pdmodel.PDDocument
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL

class FetchPDFStepTest: StepTest() {

    private val pdfCrawlerService = mock<PDFCrawlerService>()

    private val document = mock<PDDocument>()

    private val step = FetchPDFStep(pdfCrawlerService)

    private val task = IndexTask(
        source = "test",
        details = IndexTaskDetails(
            id = "123",
            taskRunId = "1234",
            pageUrl = URL("https://francisbaileyh.com/test.pdf"),
            taskType = IndexTaskType.PDF,
            refreshIntervalSeconds = 123L,
            submitTime = 1234L
        )
    )

    @Test
    fun `returns document when fetch succeeds`() {
        whenever(pdfCrawlerService.get(any())).thenReturn(document)

        val item = PipelineItem<PDDocument>(task, null)

        val result = step.process(item, monitor)

        assertEquals(document, result.payload)
        assertTrue(result.continueProcessing)
    }

    @Test
    fun `can retry on retryable exceptions`() {
        whenever(pdfCrawlerService.get(any())).thenThrow(RetryableEntityException("test"))

        val item = PipelineItem<PDDocument>(task, null)

        val result = step.process(item, monitor)

        assertFalse(result.continueProcessing)
        assertTrue(result.canRetry)
    }

    @Test
    fun `does not continue processing on any exception`() {
        whenever(pdfCrawlerService.get(any())).thenThrow(RetryableEntityException("test"))

        val item = PipelineItem<PDDocument>(task, null)

        val result = step.process(item, monitor)

        assertFalse(result.continueProcessing)
        assertTrue(result.canRetry)
    }

}