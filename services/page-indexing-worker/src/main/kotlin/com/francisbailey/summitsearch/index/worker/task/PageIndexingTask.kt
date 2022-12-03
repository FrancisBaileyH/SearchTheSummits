package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import mu.KotlinLogging
import java.net.URL


class PageIndexingTask(
    val queueName: String,
    private val pageCrawlerService: PageCrawlerService,
    private val indexingTaskQueuePollingClient: IndexingTaskQueuePollingClient,
    private val indexService: SummitSearchIndexService,
    private val indexingTaskRateLimiter: RateLimiter<String>,
    private val linkDiscoveryService: LinkDiscoveryService
): Runnable {

    private val log = KotlinLogging.logger { }

    /**
     * @TODO add a back pressure mechanism for 50X errors
     * @TODO add a feedback mechanism for non HTML page exceptions
     */
    override fun run() {
        log.info { "Running indexing task for: $queueName" }

        if (indexingTaskRateLimiter.tryConsume(queueName)) {
            val indexTask = indexingTaskQueuePollingClient.pollTask(queueName)

            if (indexTask != null) {
                processTask(indexTask)
            }
        } else {
            log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
        }
    }

    private fun processTask(task: IndexTask) {
        val pageUrl = URL(task.details.pageUrl)

        try {
            log.info { "Found indexing task for: $queueName. Fetching Page: $pageUrl" }
            val document = pageCrawlerService.getHtmlDocument(pageUrl)
            val organicLinks = document.body().getLinks()

            linkDiscoveryService.submitDiscoveries(task, organicLinks)
            indexService.indexPageContents(SummitSearchIndexRequest(
                source = pageUrl,
                htmlDocument = document
            ))

            log.info { "Successfully completed indexing task for: $queueName with $pageUrl" }
        } catch (e: Exception) {
            when (e) {
                is PermanentNonRetryablePageException -> {
                    log.error(e) { "Removing invalid content from index for: $pageUrl" }
                    indexService.deletePageContents(SummitSearchDeleteIndexRequest(pageUrl))
                }
                is RetryablePageException -> {
                    log.error(e) { "Unable to retrieve HTML content from: $pageUrl. Trying again later" }
                    log.warn { "Missing feedback/backpressure mechanism. Doing nothing." }
                }
                else -> throw e
            }
        } finally {
            indexingTaskQueuePollingClient.deleteTask(task)
        }
    }

}