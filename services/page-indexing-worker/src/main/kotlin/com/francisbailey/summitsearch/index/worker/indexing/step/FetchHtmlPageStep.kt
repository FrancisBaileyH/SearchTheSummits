package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.Discovery
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
    private val indexService: SummitSearchIndexService
): Step<Document> {

    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        return try {
            monitor.meter.timer("$metricPrefix.latency.page", "host", entity.task.details.pageUrl.host).recordCallable {
                monitor.sourceCircuitBreaker.executeCallable {
                    getDocument(entity, monitor)
                }
            }!!
        } catch (e: Exception) {
            log.error(e) { "Failed to fetch page: ${entity.task.details.pageUrl}" }
            entity.apply { canRetry = true }
            entity.apply { continueProcessing = false }
        }
    }

    private fun getDocument(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        return try {
            val document = pageCrawlerService.get(entity.task.details.pageUrl)
            entity.apply { payload = document }
        } catch (e: RedirectedEntityException) {
            e.location?.run {
                monitor.meter.counter("$metricPrefix.redirects").increment()
                linkDiscoveryService.submitDiscoveries(entity.task, listOf(Discovery(IndexTaskType.HTML, this)))
            }
            entity.apply { continueProcessing = false }
        } catch (e: NonRetryableEntityException) {
            monitor.dependencyCircuitBreaker.executeCallable {
                indexService.deletePageContents(SummitSearchDeleteIndexRequest(entity.task.details.pageUrl))
                monitor.meter.counter("$metricPrefix.indexservice.delete").increment()
                log.error(e) { "Unable to index page: ${entity.task.details.pageUrl}" }
            }
            entity.apply { continueProcessing = false }
        }
    }
}