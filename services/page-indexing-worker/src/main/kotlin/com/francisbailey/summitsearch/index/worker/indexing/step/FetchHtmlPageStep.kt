package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.lang.Exception


@Component
class FetchHtmlPageStep(
    private val pageCrawlerService: PageCrawlerService,
    private val linkDiscoveryService: LinkDiscoveryService,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<Document> {

    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        return try {
            val document = monitor.meter.timer("$metricPrefix.latency.page", "host", entity.task.details.pageUrl.host).recordCallable {
                monitor.sourceCircuitBreaker.executeCallable {
                    pageCrawlerService.get(entity.task.details.pageUrl)
                }
            }

            entity.apply { payload = document }
        } catch (e: Exception) {
            when (e) {
                is RedirectedEntityException -> {
                    e.location?.run {
                        monitor.meter.counter("$metricPrefix.redirects").increment()
                        linkDiscoveryService.submitDiscoveries(entity.task, listOf(this))
                    }
                }
                is NonRetryableEntityException -> {
                    monitor.dependencyCircuitBreaker.executeCallable {
                        summitSearchIndexService.deletePageContents(SummitSearchDeleteIndexRequest(entity.task.details.pageUrl))
                        monitor.meter.counter("$metricPrefix.indexservice.delete").increment()
                        log.error(e) { "Unable to index page: ${entity.task.details.pageUrl}" }
                    }
                }
                else -> {
                    log.error(e) { "Failed to fetch page: ${entity.task.details.pageUrl}" }
                    entity.apply { canRetry = true }
                }
            }

            entity.apply { continueProcessing = false }
        }
    }
}