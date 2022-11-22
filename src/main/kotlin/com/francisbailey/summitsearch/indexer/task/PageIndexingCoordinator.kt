package com.francisbailey.summitsearch.indexer.task

import com.francisbailey.summitsearch.indexer.IndexingQueueProvider
import com.francisbailey.summitsearch.indexer.client.PageCrawlerService
import com.francisbailey.summitsearch.indexer.client.SearchIndexService
import com.francisbailey.summitsearch.indexer.client.TaskQueuePollingClient
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.Executor

@Component
class PageIndexingCoordinator(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val indexingTaskExecutor: Executor,
    private val indexingTaskRateLimiter: RateLimiter<String>,
    private val taskQueuePollingClient: TaskQueuePollingClient,
    private val searchIndexService: SearchIndexService,
    private val pageCrawlerService: PageCrawlerService
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
                    taskQueuePollingClient = taskQueuePollingClient,
                    indexService = searchIndexService,
                    indexingTaskRateLimiter = indexingTaskRateLimiter
                )
            )
        }
    }
}