package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.util.*

class PageIndexingTaskTest {

    private val queueName = "IndexQueue-Test1"
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexingTaskQueuePollingClient = mock<IndexingTaskQueuePollingClient>()
    private val indexService = mock<SummitSearchIndexService>()
    private val indexingTaskRateLimiter = mock<RateLimiter<String>>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val task = PageIndexingTask(
        queueName,
        pageCrawlerService,
        indexingTaskQueuePollingClient,
        indexService,
        indexingTaskRateLimiter,
        linkDiscoveryService
    )

    private val defaultIndexTask = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = "https://www.francisbaileyh.com",
            submitTime = Date().time,
            taskRunId = "test123"
        )
    )

    @Test
    fun `skip execution if rate limit exceeded`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(false)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)

        verifyNoInteractions(indexingTaskQueuePollingClient)
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
    }

    @Test
    fun `skip indexing if no tasks are present on queue`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(null)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
    }

    @Test
    fun `crawl page and index contents if rate limit not exceeded and task is present on queue`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(URL(defaultIndexTask.details.pageUrl))).thenReturn(htmlContent)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(indexService).indexPageContents(SummitSearchIndexRequest(URL(defaultIndexTask.details.pageUrl), htmlContent))
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
    }

    @Test
    fun `deletes page contents from index when 40X error or 30X response is encountered`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(any())).thenThrow(PermanentNonRetryablePageException("test"))

        task.run()

        verify(indexService, never()).indexPageContents(any())
        verify(indexService).deletePageContents(eq(SummitSearchDeleteIndexRequest(source = URL(defaultIndexTask.details.pageUrl))))
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
    }

    @Test
    fun `only deletes task when retryable exceptions occur`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(any())).thenThrow(RetryablePageException("test"))

        task.run()

        verifyNoInteractions(indexService)
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
    }

}