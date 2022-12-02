package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.IndexingQueueProvider
import com.francisbailey.summitsearch.index.worker.client.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class PageIndexingTaskCoordinator(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val indexingTaskExecutor: Executor,
    private val indexingTaskRateLimiter: RateLimiter<String>,
    private val indexingTaskQueuePollingClient: IndexingTaskQueuePollingClient,
    private val summitSearchIndexService: SummitSearchIndexService,
    private val pageCrawlerService: PageCrawlerService,
    private val linkDiscoveryService: LinkDiscoveryService
) {
    private val log = KotlinLogging.logger { }

    /**
     * Each Website has new pages added to a per-site queue. To respect crawling requirements
     * We will iterate through all queues periodically, test the rate limiter and create
     * a task to fetch once from the queue and process/index the web page.
     *
     * If we need to have per-site crawling limits, the rate limiting can be handled by the IndexingQueueProvider
     */
    fun coordinateTaskExecution() {
        log.info { "Running indexing tasks now" }
        indexingQueueProvider.getQueues().forEach { queue ->
            indexingTaskExecutor.execute(
                PageIndexingTask(
                    queueName = queue,
                    pageCrawlerService = pageCrawlerService,
                    indexingTaskQueuePollingClient = indexingTaskQueuePollingClient,
                    indexService = summitSearchIndexService,
                    indexingTaskRateLimiter = indexingTaskRateLimiter,
                    linkDiscoveryService = linkDiscoveryService
                )
            )
        }
    }
}