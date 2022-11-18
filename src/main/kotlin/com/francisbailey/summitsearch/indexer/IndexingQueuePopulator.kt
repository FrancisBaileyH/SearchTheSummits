package com.francisbailey.summitsearch.indexer

import com.francisbailey.summitsearch.indexer.client.PageCrawlerService
import com.francisbailey.summitsearch.indexer.client.SearchIndexService
import com.francisbailey.summitsearch.indexer.client.TaskQueueClient
import com.francisbailey.summitsearch.indexer.client.TaskQueuePollingClient
import com.francisbailey.summitsearch.indexer.task.PageIndexingTask
import com.francisbailey.summitsearch.indexer.task.RateLimiter
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.Executor


@Component
class IndexingQueueProvider(
    private val taskQueueClient: TaskQueueClient
){
    private val log = KotlinLogging.logger { }

    private val queues = mutableSetOf<String>()

    fun refreshQueues() {
        log.info { "Refreshing indexing task queues" }

        val refreshedQueues = taskQueueClient.listTaskQueues()

        log.info { "Found ${refreshedQueues.size} indexing task queues on last refresh" }

        synchronized(queues) {
            queues.clear()
            queues.addAll(refreshedQueues)
        }
    }

    fun getQueues(): Set<String> = synchronized(queues) { queues }
}



