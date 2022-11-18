package com.francisbailey.summitsearch.indexer.task

import com.francisbailey.summitsearch.indexer.client.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.util.*

class PageIndexingTaskTest {

    private val queueName = "IndexQueue-Test1"
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val taskQueuePollingClient = mock<TaskQueuePollingClient>()
    private val indexService = mock<SearchIndexService>()
    private val indexingTaskRateLimiter = mock<RateLimiter<String>>()


    private val task = PageIndexingTask(queueName, pageCrawlerService, taskQueuePollingClient, indexService, indexingTaskRateLimiter)


    @Test
    fun `skip execution if rate limit exceeded`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(false)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)

        verifyNoInteractions(taskQueuePollingClient)
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
    }

    @Test
    fun `skip indexing if no tasks are present on queue`() {
        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(taskQueuePollingClient.pollTask(queueName)).thenReturn(null)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)
        verify(taskQueuePollingClient).pollTask(queueName)
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
    }

    @Test
    fun `crawl page and index contents if rate limit not exceeded and task is present on queue`() {
        val testTask = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = "https://www.francisbaileyh.com",
                submitTime = Date().time
            )
        )

        val htmlContent = "<html>Some Web Page</html>"

        whenever(indexingTaskRateLimiter.tryConsume(queueName)).thenReturn(true)
        whenever(taskQueuePollingClient.pollTask(queueName)).thenReturn(testTask)
        whenever(pageCrawlerService.getHtmlContentAsString(URL(testTask.details.pageUrl))).thenReturn(htmlContent)

        task.run()

        verify(indexingTaskRateLimiter).tryConsume(queueName)
        verify(taskQueuePollingClient).pollTask(queueName)
        verify(indexService).indexPageContents(URL(testTask.details.pageUrl), htmlContent)
        verify(taskQueuePollingClient).deleteTask(testTask)
    }

}