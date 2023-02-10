package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchHtmlPageStep
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexHtmlPageRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class FetchHtmlPageStepTest: StepTest() {

    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexService = mock<SummitSearchIndexService>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = FetchHtmlPageStep(
        pageCrawlerService = pageCrawlerService,
        linkDiscoveryService = linkDiscoveryService,
        indexService = indexService
    )

    private val pipelineItem = PipelineItem<Document>(
        task = defaultIndexTask,
        payload = null
    )


    @Test
    fun `crawl pages and returns contents`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(pageCrawlerService.get(defaultIndexTask.details.pageUrl)).thenReturn(htmlContent)

        val result = step.process(pipelineItem, monitor)

        assertEquals(htmlContent, result.payload)
        assertTrue(result.continueProcessing)
        assertFalse(result.canRetry)

        verify(perQueuecircuitBreaker).executeCallable<Unit>(any())
        verifyNoInteractions(indexService)
    }

    @Test
    fun `deletes page contents from index when 40X error is encountered`() {
        whenever(pageCrawlerService.get(any())).thenThrow(NonRetryableEntityException("test"))

        val result = step.process(pipelineItem, monitor)

        assertFalse(result.continueProcessing)
        assertFalse(result.canRetry)

        verify(indexService, never()).indexPageContents(any<SummitSearchIndexHtmlPageRequest>())
        verify(indexService).deletePageContents(eq(SummitSearchDeleteIndexRequest(source = defaultIndexTask.details.pageUrl)))
    }

    @Test
    fun `does not delete task when retryable exceptions occur`() {
        whenever(pageCrawlerService.get(any())).thenThrow(RetryableEntityException("test"))

        val result = step.process(pipelineItem, monitor)

        assertFalse(result.continueProcessing)
        assertTrue(result.canRetry)

        verifyNoInteractions(indexService)
    }

    @Test
    fun `submits discovery on page redirected exception`() {
        val location = "https://some-site.com/redirect/here"

        whenever(pageCrawlerService.get(any())).thenThrow(RedirectedEntityException(location, "test"))

        val result = step.process(pipelineItem, monitor)


        assertFalse(result.continueProcessing)
        assertFalse(result.canRetry)

        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, listOf(location))
        verifyNoInteractions(indexService)
    }
}