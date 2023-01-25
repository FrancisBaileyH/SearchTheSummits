package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration
import java.util.*

class IndexingTaskTest {

    private val queueName = "IndexQueue-Test1"
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val indexingTaskQueuePollingClient = mock<IndexingTaskQueuePollingClient>()
    private val indexService = mock<SummitSearchIndexService>()

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

    private val indexingPipeline = mock<Pipeline>()

    private val taskPermit = mock<TaskPermit>()

    private val task = IndexingTask(
        queueName,
        indexingTaskQueuePollingClient,
        indexingPipeline,
        rateLimiterRegistry,
        depencencyCircuitBreaker,
        perQueuecircuitBreaker,
        taskPermit,
        SimpleMeterRegistry()
    )

    private val defaultIndexTask = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.HTML,
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
    fun `run indexing pipeline if rate limit not exceeded and task is present on queue`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(indexingPipeline.process(any(), any())).thenReturn(false)

        task.run()

        verify(rateLimiterRegistry).rateLimiter(task.queueName)
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(depencencyCircuitBreaker, times(2)).executeCallable<Unit>(any())
        verify(indexingPipeline).process(eq(defaultIndexTask), any())
        verify(indexingTaskQueuePollingClient).deleteTask(defaultIndexTask)
        verify(taskPermit).close()
    }

    @Test
    fun `does not delete task if should retry is true`() {
        whenever(rateLimiter.acquirePermission()).thenReturn(true)
        whenever(indexingTaskQueuePollingClient.pollTask(queueName)).thenReturn(defaultIndexTask)
        whenever(indexingPipeline.process(any(), any())).thenReturn(true)

        task.run()

        verify(rateLimiterRegistry).rateLimiter(task.queueName)
        verify(indexingTaskQueuePollingClient).pollTask(queueName)
        verify(depencencyCircuitBreaker).executeCallable<Unit>(any())
        verify(indexingPipeline).process(eq(defaultIndexTask), any())
        verifyNoMoreInteractions(indexingTaskQueuePollingClient)
        verify(taskPermit).close()
    }
}