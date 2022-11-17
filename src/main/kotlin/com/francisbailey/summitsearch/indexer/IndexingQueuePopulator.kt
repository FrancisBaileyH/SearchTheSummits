package com.francisbailey.summitsearch.indexer

import com.francisbailey.summitsearch.indexer.client.SearchIndexClient
import com.francisbailey.summitsearch.indexer.client.TaskQueueClient
import com.francisbailey.summitsearch.indexer.client.TaskQueuePollingClient
import com.francisbailey.summitsearch.indexer.task.RateLimiter
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URL
import java.util.concurrent.Executor


@Component
class IndexingQueueProvider(
    private val taskQueueClient: TaskQueueClient
){
    private val log = KotlinLogging.logger { }

    private val queues = mutableSetOf<String>()

    @Scheduled(fixedRate = 10000)
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



@Component
class PageIndexingCoordinator(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val indexingTaskExecutor: Executor,
    private val indexingTaskRateLimiter: RateLimiter<String>,
    private val taskQueuePollingClient: TaskQueuePollingClient,
    private val searchIndexClient: SearchIndexClient,
    private val httpClient: HttpClient
) {
    private val log = KotlinLogging.logger { }

    /**
     * Each Website has new pages added to a per-site queue. To respect crawling requirements
     * We will iterate through all queues periodically, test the rate limiter and create
     * a task to fetch once from the queue and process/index the web page.
     *
     * If we need to have per-site crawling limits, the rate limiting can be handled by the IndexingQueueProvider
     */
    @Scheduled(fixedRate = 100)
    fun start() {
        log.info { "Running indexing tasks now" }
        indexingQueueProvider.getQueues().forEach {
            indexingTaskExecutor.execute(
                PageIndexingTask(
                    queueName = it,
                    httpClient = httpClient,
                    taskQueuePollingClient = taskQueuePollingClient,
                    indexClient = searchIndexClient,
                    indexingTaskRateLimiter = indexingTaskRateLimiter
                )
            )
        }
    }
}


/**
 * At some point we'll need a mechanism for handling 404's on a given link due to a bad SiteMap generation
 *
 * @TODO Need queue deletion mechanism
 */
class PageIndexingTask(
    private val queueName: String,
    private val httpClient: HttpClient,
    private val taskQueuePollingClient: TaskQueuePollingClient,
    private val indexClient: SearchIndexClient,
    private val indexingTaskRateLimiter: RateLimiter<String>
): Runnable {

    private val log = KotlinLogging.logger { }

    override fun run() {
        log.info { "Running indexing task for: $queueName" }

        if (indexingTaskRateLimiter.tryConsume(queueName)) {
            val indexTask = taskQueuePollingClient.pollTask(queueName)

            if (indexTask != null) {
                log.info { "Found indexing task for: $queueName. Fetching Page: ${indexTask.details.pageUrl}" }

                runBlocking {
                    val response = httpClient.get(URL(indexTask.details.pageUrl))
                    check(response.status == HttpStatusCode.OK) {
                        "Unexpected response when fetching from: ${indexTask.details.pageUrl}"
                    }
                    indexClient.indexPageContents(response.bodyAsText())
                }

                taskQueuePollingClient.deleteTask(indexTask)
                log.info { "Successfully completed indexing task for: $queueName" }
            }
        } else {
            log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
        }
    }

}