package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.PermanentNonRetryablePageException
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.net.URL


class PageIndexingTask(
    val queueName: String,
    private val pageCrawlerService: PageCrawlerService,
    private val indexingTaskQueuePollingClient: IndexingTaskQueuePollingClient,
    private val indexService: SummitSearchIndexService,
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val dependencyCircuitBreaker: CircuitBreaker,
    private val perQueueCircuitBreaker: CircuitBreaker,
    private val linkDiscoveryService: LinkDiscoveryService,
    private val taskPermit: TaskPermit,
    private val meterRegistry: MeterRegistry
): Runnable {

    private val log = KotlinLogging.logger { }


    override fun run() = taskPermit.use {
        log.info { "Running indexing task for: $queueName" }

        if (rateLimiterRegistry.rateLimiter(queueName).acquirePermission()) {
            val indexTask: IndexTask? = dependencyCircuitBreaker.executeCallable {
                indexingTaskQueuePollingClient.pollTask(queueName)
            }

            if (indexTask != null) {
                perQueueCircuitBreaker.executeCallable {
                    processTask(indexTask)
                }
            }
        } else {
            meterRegistry.counter("task.indexing.rate-limited").increment()
            log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
        }
    }

    private fun processTask(task: IndexTask) {
        val pageUrl = URL(task.details.pageUrl)
        log.info { "Found indexing task for: $queueName. Fetching Page: $pageUrl" }

        try {
            val document = meterRegistry.timer("task.indexing.latency.host.${pageUrl.host}").recordCallable {
                pageCrawlerService.getHtmlDocument(pageUrl)
            }!!

            val organicLinks = document.body().getLinks()

            dependencyCircuitBreaker.executeCallable {

                linkDiscoveryService.submitDiscoveries(task, organicLinks)
                meterRegistry.timer("task.indexing.indexservice.latency").recordCallable {
                    indexService.indexPageContents(
                        SummitSearchIndexRequest(
                            source = pageUrl,
                            htmlDocument = document
                        )
                    )
                }

                log.info { "Successfully completed indexing task for: $queueName with $pageUrl" }
                meterRegistry.counter("task.indexing.success").increment()
                meterRegistry.counter("task.indexing.success.host.${pageUrl.host}").increment()
                meterRegistry.counter("task.indexing.links.discovered").increment(organicLinks.size.toDouble())
            }
        } catch (e: PermanentNonRetryablePageException) {
            log.error(e) { "Removing invalid content from index for: $pageUrl" }

            dependencyCircuitBreaker.executeCallable {
                indexService.deletePageContents(SummitSearchDeleteIndexRequest(pageUrl))
            }
        } finally {
            dependencyCircuitBreaker.executeCallable {
                indexingTaskQueuePollingClient.deleteTask(task)
            }
        }
    }

}