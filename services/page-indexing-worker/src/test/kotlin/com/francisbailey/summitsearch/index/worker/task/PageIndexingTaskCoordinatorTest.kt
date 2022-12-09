package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.services.common.RateLimiter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.concurrent.Executor

class PageIndexingTaskCoordinatorTest {

    private val queueAssignmentStore = mock<QueueAssignmentStore>()
    private val executor = mock<Executor>()
    private val indexingTaskRateLimiter = mock<RateLimiter<String>>()
    private val indexingTaskQueuePollingClient = mock<IndexingTaskQueuePollingClient>()
    private val searchIndexService = mock<SummitSearchIndexService>()
    private val pageCrawlerService = mock<PageCrawlerService>()
    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val indexingCoordinator = PageIndexingTaskCoordinator(
        queueAssignmentStore = queueAssignmentStore,
        indexingTaskExecutor = executor,
        indexingTaskRateLimiter = indexingTaskRateLimiter,
        indexingTaskQueuePollingClient = indexingTaskQueuePollingClient,
        summitSearchIndexService = searchIndexService,
        pageCrawlerService = pageCrawlerService,
        linkDiscoveryService
    )


    @Test
    fun `iterates through queues if there are any and adds task to executor`() {
        val queues = setOf("QueueA", "QueueB", "QueueC")

        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        queues.forEach { _ ->
            verify(executor, times(queues.size)).execute(any<PageIndexingTask>())
        }
    }

    @Test
    fun `does not execute tasks if there are no queues`() {
        val queues = emptySet<String>()
        whenever(queueAssignmentStore.getAssignments()).thenReturn(queues)

        indexingCoordinator.coordinateTaskExecution()

        verifyNoInteractions(executor)
    }
}