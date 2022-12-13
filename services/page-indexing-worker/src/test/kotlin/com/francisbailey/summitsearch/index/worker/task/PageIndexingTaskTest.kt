package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.PermanentNonRetryablePageException
import com.francisbailey.summitsearch.index.worker.crawler.RetryablePageException
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration
import java.util.*

class PageIndexingTaskTest {

    private val queueName = "IndexQueue-Test1"
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexingTaskQueuePollingClient = mock<IndexingTaskQueuePollingClient>()
    private val indexService = mock<SummitSearchIndexService>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val rateLimiter = mock<io.github.resilience4j.ratelimiter.RateLimiter>()

    private val rateLimiterRegistry= mock<RateLimiterRegistry> {
        on(mock.rateLimiter(any())).thenReturn(rateLimiter)
    }

    private val perQueuecircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }
    private val depencencyCircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }

    private val taskPermit = mock<TaskPermit>()

    private val task = PageIndexingTask(
        queueName,
        pageCrawlerService,
        indexingTaskQueuePollingClient,
        indexService,
        rateLimiterRegistry,
        depencencyCircuitBreaker,
        perQueuecircuitBreaker,
        linkDiscoveryService,
        taskPermit,
        SimpleMeterRegistry()
    )

    private val defaultIndexTask = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = "https://www.francisbaileyh.com",
            submitTime = Date().time,
            taskRunId = "test123",
            refreshIntervalSeconds = Duration.ofMinutes(60).seconds
        )
    )

    @Test
    fun `skip execution if rate limit exceeded`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(false)

        task.run()

        verify(rateLimiterRegistry).rateLimiter(task.queueName)
        verify(rateLimiter).acquirePermission()

        verifyNoInteractions(indexingTaskQueuePollingClient)
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
        verify(taskPermit).close()
    }

    @Test
    fun `skip indexing if no tasks are present on queue`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(null)
        whenever(depencencyCircuitBreaker.executeCallable<IndexTask?>(any())).thenCallRealMethod()

        task.run()

        verify(rateLimiterRegistry).rateLimiter(task.queueName)
        verify(rateLimiter).acquirePermission()
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(depencencyCircuitBreaker).executeCallable<IndexTask?>(any())
        verifyNoInteractions(indexService)
        verifyNoInteractions(pageCrawlerService)
        verify(taskPermit).close()
    }

    @Test
    fun `crawl page and index contents if rate limit not exceeded and task is present on queue`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(URL(defaultIndexTask.details.pageUrl))).thenReturn(htmlContent)

        task.run()

        verify(rateLimiterRegistry).rateLimiter(task.queueName)
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(depencencyCircuitBreaker, times(3)).executeCallable<Unit>(any())
        verify(indexService).indexPageContents(SummitSearchIndexRequest(URL(defaultIndexTask.details.pageUrl), htmlContent))
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
        verify(taskPermit).close()
    }

    @Test
    fun `submit new links for discovery if any are found`() {
        val links = listOf("https://francisbailey.com/test", "https://francisbailey.com/test2")
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        links.forEach {
            htmlContent.body().appendElement("a")
                .attr("href", it)
                .text(it)
        }

        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(URL(defaultIndexTask.details.pageUrl))).thenReturn(htmlContent)

        task.run()

        verify(rateLimiter).acquirePermission()
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, links)
        verify(indexService).indexPageContents(SummitSearchIndexRequest(URL(defaultIndexTask.details.pageUrl), htmlContent))
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
        verify(taskPermit).close()
    }

    @Test
    fun `deletes page contents from index when 40X error or 30X response is encountered`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(any())).thenThrow(PermanentNonRetryablePageException("test", Exception("test")))

        task.run()

        verify(indexService, never()).indexPageContents(any())
        verify(indexService).deletePageContents(eq(SummitSearchDeleteIndexRequest(source = URL(defaultIndexTask.details.pageUrl))))
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
        verify(taskPermit).close()
    }

    @Test
    fun `only deletes task when retryable exceptions occur`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(pageCrawlerService.getHtmlDocument(any())).thenThrow(RetryablePageException("test", Exception("test")))

        assertThrows<RetryablePageException> { task.run() }

        verifyNoInteractions(indexService)
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
        verify(taskPermit).close()
    }

}