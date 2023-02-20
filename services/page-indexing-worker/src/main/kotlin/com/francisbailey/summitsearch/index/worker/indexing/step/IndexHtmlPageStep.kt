package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.htmldate.GoodEnoughHtmlDateGuesser
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexHtmlPageRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class IndexHtmlPageStep(
    private val htmlDateGuesser: GoodEnoughHtmlDateGuesser,
    private val documentIndexingFilterService: DocumentFilterService,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<Document> {

    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        if (documentIndexingFilterService.shouldFilter(entity.task.details.pageUrl)) {
            log.warn { "Skipping indexing on: ${entity.task.details.pageUrl} as it matches filter" }
            return entity
        }

        val host = entity.task.details.pageUrl.host
        val date = monitor.meter.timer("${metricPrefix}.dateguess.latency", "host", host).recordCallable {
            htmlDateGuesser.findDate(entity.task.details.pageUrl, entity.payload!!)
        }

        if (date == null) {
            monitor.meter.counter("${metricPrefix}.dateguess.miss", "host", host).increment()
        }

        monitor.meter.timer("$metricPrefix.indexservice.add.latency").recordCallable {
            monitor.dependencyCircuitBreaker.executeCallable {
                summitSearchIndexService.indexPageContents(
                    SummitSearchIndexHtmlPageRequest(
                        source = entity.task.details.pageUrl,
                        htmlDocument = entity.payload!!,
                        pageCreationDate = date
                    )
                )
            }
        }

        log.info { "Successfully completed indexing task for: ${entity.task.source} with ${entity.task.details.pageUrl}" }
        monitor.meter.counter("$metricPrefix.indexservice.add.success", "host" , host).increment()

        return entity
    }
}