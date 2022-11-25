package com.francisbailey.summitsearch.indexer.task

import com.francisbailey.summitsearch.indexer.IndexingQueueProvider
import com.francisbailey.summitsearch.indexer.client.PageCrawlerService
import com.francisbailey.summitsearch.indexer.client.TaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.concurrent.Executor

class PageIndexingCoordinatorTest {

    private val indexingQueueProvider = mock<IndexingQueueProvider>()
    private val executor = mock<Executor>()
    private val indexingTaskRateLimiter = mock<RateLimiter<String>>()
    private val taskQueuePollingClient = mock<TaskQueuePollingClient>()
    private val searchIndexService = mock<SummitSearchIndexService>()
    private val pageCrawlerService = mock<PageCrawlerService>()

    private val indexingCoordinator = PageIndexingCoordinator(
        indexingQueueProvider = indexingQueueProvider,
        indexingTaskExecutor = executor,
        indexingTaskRateLimiter = indexingTaskRateLimiter,
        taskQueuePollingClient = taskQueuePollingClient,
        summitSearchIndexService = searchIndexService,
        pageCrawlerService = pageCrawlerService
    )


    @Test
    fun `iterates through queues if there are any and adds task to executor`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")

        whenever(indexingQueueProvider.getQueues()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        queues.forEach { _ ->
            verify(executor, times(queues.size)).execute(any<PageIndexingTask>())
        }
    }

    @Test
    fun `does not execute tasks if there are no queues`() {
        val queues = emptySet<String>()
        whenever(indexingQueueProvider.getQueues()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        verifyNoInteractions(executor)
    }
}