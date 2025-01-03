package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.htmldate.GoodEnoughHtmlDateGuesser
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.*
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchHtmlPageStep
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.francisbailey.summitsearch.indexservice.DocumentDeleteRequest
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import io.ktor.http.*
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime

class FetchHtmlPageStepTest: StepTest() {

    private val htmlDateGuesser = mock<GoodEnoughHtmlDateGuesser>()
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexService = mock<DocumentIndexService>()
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

        whenever(pageCrawlerService.get(defaultIndexTask.details.entityUrl)).thenReturn(htmlContent)

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

        verify(indexService, never()).indexContent(any<DocumentPutRequest>())
        verify(indexService).deletePageContents(eq(DocumentDeleteRequest(source = defaultIndexTask.details.entityUrl)))
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
    fun `submits discovery on pdf content discovery`() {
        whenever(pageCrawlerService.get(any())).thenThrow(UnsupportedEntityException(ContentType.Application.Pdf, "TEST"))

        assertThrows<NonRetryableEntityException> { step.process(pipelineItem, monitor) }

        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, listOf(Discovery(IndexTaskType.PDF, pipelineItem.task.details.entityUrl.toString(), skipCacheCheck = true)))
        verifyNoInteractions(indexService)
    }

    @Test
    fun `supplies date if a date guess returns result`() {
        val date = LocalDateTime.now()
        whenever(htmlDateGuesser.findDate(any(), any())).thenReturn(date)

        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(pageCrawlerService.get(defaultIndexTask.details.entityUrl)).thenReturn(htmlContent)

        val result = step.process(pipelineItem, monitor)

        assertEquals(htmlContent, result.payload!!.document)
        assertEquals(date, result.payload!!.pageCreationDate)
        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)

        verify(perQueuecircuitBreaker).executeCallable<Unit>(any())
        verifyNoInteractions(indexService)
    }
}