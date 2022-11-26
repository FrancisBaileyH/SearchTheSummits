package com.francisbailey.summitsearch.index.worker.task.task

import com.francisbailey.summitsearch.index.worker.task.client.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.task.client.TaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import mu.KotlinLogging
import java.net.URL


/**
 * At some point we'll need a mechanism for handling 404's on a given link due to a bad SiteMap generation
 *
 * @TODO Need queue deletion mechanism
 */

class PageIndexingTask(
    val queueName: String,
    private val pageCrawlerService: PageCrawlerService,
    private val taskQueuePollingClient: TaskQueuePollingClient,
    private val indexService: SummitSearchIndexService,
    private val indexingTaskRateLimiter: RateLimiter<String>
): Runnable {

    private val log = KotlinLogging.logger { }

    override fun run() {
        log.info { "Running indexing task for: $queueName" }

        if (indexingTaskRateLimiter.tryConsume(queueName)) {
            val indexTask = taskQueuePollingClient.pollTask(queueName)

            if (indexTask != null) {
                val pageUrl = URL(indexTask.details.pageUrl)
                log.info { "Found indexing task for: $queueName. Fetching Page: $pageUrl" }

                val htmlContent = try {
                    pageCrawlerService.getHtmlContentAsString(pageUrl)
                } catch (e: Exception) {
                    log.error(e) { "Unable retrieve HTML content from: $pageUrl" }
                    return
                }

                indexService.indexPageContents(SummitSearchIndexRequest(
                    source = pageUrl,
                    htmlContent = htmlContent
                ))

                taskQueuePollingClient.deleteTask(indexTask)
                log.info { "Successfully completed indexing task for: $queueName with $pageUrl" }
            }
        } else {
            log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
        }
    }

}