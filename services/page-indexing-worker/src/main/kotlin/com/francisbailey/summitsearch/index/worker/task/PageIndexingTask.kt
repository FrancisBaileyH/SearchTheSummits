package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.lang.Exception
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
    private val documentIndexingFilterService: DocumentFilterService,
    private val taskPermit: TaskPermit,
    private val meterRegistry: MeterRegistry
): Runnable {

    private val log = KotlinLogging.logger { }

    private var canDeleteTask = true

    override fun run() = taskPermit.use {
        log.info { "Running indexing task for: $queueName" }

        if (!rateLimiterRegistry.rateLimiter(queueName).acquirePermission()) {
            meterRegistry.counter("$TASK_METRIC.rate-limited").increment()
            log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
            return
        }

        val task: IndexTask? = dependencyCircuitBreaker.executeCallable {
            indexingTaskQueuePollingClient.pollTask(queueName)
        }

        if (task != null) {
            try {
                perQueueCircuitBreaker.executeCallable { processTask(task) }
            } finally {
                if (canDeleteTask) {
                    dependencyCircuitBreaker.executeCallable { indexingTaskQueuePollingClient.deleteTask(task) }
                } else {
                    log.info { "Returning task to queue: $queueName" }
                }
            }
        }
    }

    /**
     * IndexingPipeline
     * - fetch
     * - filter
     * - transform
     * - index
     *
     */
    private fun processTask(task: IndexTask) {
        val pageUrl = URL(task.details.pageUrl)

        try {
            val timer = meterRegistry.timer("$TASK_METRIC.latency.page", "host", pageUrl.host)
            val document = timer.recordCallable { pageCrawlerService.get(pageUrl) }!!
            val organicLinks = document.body().getLinks()

            linkDiscoveryService.submitDiscoveries(task, organicLinks)
            // should filter entity
            if (documentIndexingFilterService.shouldFilter(pageUrl)) {
                log.warn { "Crawled, but did not index page: $pageUrl" }
                return
            }

            dependencyCircuitBreaker.executeCallable {
                meterRegistry.timer("$TASK_METRIC.indexservice.add.latency").recordCallable {
                    indexService.indexPageContents(
                        SummitSearchIndexRequest(
                            source = pageUrl,
                            htmlDocument = document
                        )
                    )
                }

                log.info { "Successfully completed indexing task for: $queueName with $pageUrl" }
                meterRegistry.counter("$TASK_METRIC.success", "host" , pageUrl.host).increment()
                meterRegistry.counter("$TASK_METRIC.links.discovered").increment(organicLinks.size.toDouble())
            }
        }
        catch (e: Exception) {
            meterRegistry.counter("$TASK_METRIC.exception", "type", e.javaClass.simpleName, "queue", queueName).increment()

            when (e) {
                is RedirectedEntityException -> {
                    e.location?.run {
                        meterRegistry.counter("$TASK_METRIC.redirects").increment()
                        linkDiscoveryService.submitDiscoveries(task, listOf(this))
                    }
                }
                is NonRetryableEntityException -> {
                    dependencyCircuitBreaker.executeCallable {
                        indexService.deletePageContents(SummitSearchDeleteIndexRequest(pageUrl))
                        meterRegistry.counter("$TASK_METRIC.indexservice.delete").increment()
                        log.error(e) { "Unable to index page: $pageUrl" }
                    }
                }
                else -> {
                    canDeleteTask = false
                    throw e
                }
            }
        }
    }

    companion object {
        const val TASK_METRIC = "task.indexing"
    }

}