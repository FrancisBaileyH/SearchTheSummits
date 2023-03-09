package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.htmldate.GoodEnoughHtmlDateGuesser
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchHtmlPageStep
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchPutHtmlPageRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime

class FetchHtmlPageStepTest: StepTest() {

    private val htmlDateGuesser = mock<GoodEnoughHtmlDateGuesser>()
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexService = mock<SummitSearchIndexService>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = FetchHtmlPageStep(
        pageCrawlerService = pageCrawlerService,
        linkDiscoveryService = linkDiscoveryService,
        indexService = indexService,
        htmlDateGuesser = htmlDateGuesser
    )

    private val pipelineItem = PipelineItem<DatedDocument>(
        task = defaultIndexTask,
        payload = null
    )


    @Test
    fun `crawl pages and returns contents`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(pageCrawlerService.get(defaultIndexTask.details.pageUrl)).thenReturn(htmlContent)

        val result = step.process(pipelineItem, monitor)

        assertEquals(htmlContent, result.payload!!.document)
        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)

        verify(perQueuecircuitBreaker).executeCallable<Unit>(any())
        verifyNoInteractions(indexService)
    }

    @Test
    fun `deletes page contents from index when 40X error is encountered`() {
        whenever(pageCrawlerService.get(any())).thenThrow(NonRetryableEntityException("test"))

        assertThrows<NonRetryableEntityException> { step.process(pipelineItem, monitor) }

        verify(indexService, never()).indexContent(any<SummitSearchPutHtmlPageRequest>())
        verify(indexService).deletePageContents(eq(SummitSearchDeleteIndexRequest(source = defaultIndexTask.details.pageUrl)))
    }

    @Test
    fun `does not throw nonretryableexception when retryable exceptions occur`() {
        whenever(pageCrawlerService.get(any())).thenThrow(RetryableEntityException("test"))

        assertThrows<RetryableEntityException> { step.process(pipelineItem, monitor) }

        verifyNoInteractions(indexService)
    }

    @Test
    fun `submits discovery on page redirected exception`() {
        val location = "https://some-site.com/redirect/here"

        whenever(pageCrawlerService.get(any())).thenThrow(RedirectedEntityException(location, "test"))

        assertThrows<NonRetryableEntityException> { step.process(pipelineItem, monitor) }

        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, listOf(Discovery(IndexTaskType.HTML, location)))
        verifyNoInteractions(indexService)
    }

    @Test
    fun `supplies date if a date guess returns result`() {
        val date = LocalDateTime.now()
        whenever(htmlDateGuesser.findDate(any(), any())).thenReturn(date)

        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(pageCrawlerService.get(defaultIndexTask.details.pageUrl)).thenReturn(htmlContent)

        val result = step.process(pipelineItem, monitor)

        assertEquals(htmlContent, result.payload!!.document)
        assertEquals(date, result.payload!!.pageCreationDate)
        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)

        verify(perQueuecircuitBreaker).executeCallable<Unit>(any())
        verifyNoInteractions(indexService)
    }
}