package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.SummitSearchPutHtmlPageRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.springframework.stereotype.Component

@Component
class IndexHtmlPageStep(
    private val documentIndexingFilterService: DocumentFilterService,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        if (documentIndexingFilterService.shouldFilter(entity.task.details.pageUrl)) {
            log.warn { "Skipping indexing on: ${entity.task.details.pageUrl} as it matches filter" }
            return entity
        }

        monitor.meter.timer("$metricPrefix.indexservice.add.latency").recordCallable {
            monitor.dependencyCircuitBreaker.executeCallable {
                summitSearchIndexService.putPageContents(
                    SummitSearchPutHtmlPageRequest(
                        source = entity.task.details.pageUrl,
                        htmlDocument = entity.payload!!.document,
                        pageCreationDate = entity.payload!!.pageCreationDate
                    )
                )
            }
        }

        log.info { "Successfully completed indexing task for: ${entity.task.source} with ${entity.task.details.pageUrl}" }
        monitor.meter.counter("$metricPrefix.indexservice.add.success", "host" , entity.task.details.pageUrl.host).increment()

        return entity
    }
}