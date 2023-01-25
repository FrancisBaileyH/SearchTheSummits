package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.DocumentFilterService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.net.URL

@Component
class IndexHtmlPageStep(
    private val documentIndexingFilterService: DocumentFilterService,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<Document> {


    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        if (!documentIndexingFilterService.shouldFilter(entity.task.details.pageUrl)) {
            monitor.meter.timer("$metricPrefix.indexservice.add.latency").recordCallable {
                monitor.dependencyCircuitBreaker.executeCallable {
                    summitSearchIndexService.indexPageContents(
                        SummitSearchIndexRequest(
                            source = URL(entity.task.source),
                            htmlDocument = entity.payload!!
                        )
                    )
                }
            }

            log.info { "Successfully completed indexing task for: ${entity.task.source} with ${entity.task.details.pageUrl}" }
            monitor.meter.counter("$metricPrefix.success", "host" , entity.task.details.pageUrl.host).increment()
        }

        return entity
    }
}