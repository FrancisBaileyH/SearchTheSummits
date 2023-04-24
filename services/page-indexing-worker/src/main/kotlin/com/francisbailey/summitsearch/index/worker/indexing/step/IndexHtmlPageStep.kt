package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractor
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import org.springframework.stereotype.Component


@Component
class IndexHtmlPageStep(
    private val documentIndexingFilterService: DocumentFilterService,
    private val documentIndexService: DocumentIndexService,
    private val htmlContentExtractor: ContentExtractor<DocumentText>
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        if (documentIndexingFilterService.shouldFilter(entity.task.details.entityUrl)) {
            log.warn { "Skipping indexing on: ${entity.task.details.entityUrl} as it matches filter" }
            return entity
        }

        val document = entity.payload!!.document
        val entityUrl = entity.task.details.entityUrl
        val content = htmlContentExtractor.extract(entityUrl, document)

        monitor.meter.timer("indexservice.add.latency").recordCallable {
            monitor.dependencyCircuitBreaker.executeCallable {
                documentIndexService.indexContent(
                    DocumentPutRequest(
                        source = entityUrl,
                        title = content.title.ifBlank { entityUrl.host },
                        seoDescription = content.description,
                        paragraphContent = content.semanticText,
                        rawTextContent = content.rawText,
                        pageCreationDate = entity.payload!!.pageCreationDate
                    )
                )
            }
        }

        log.info { "Successfully completed indexing task for: ${entity.task.source} with ${entity.task.details.entityUrl}" }
        monitor.meter.counter("indexservice.add.success", "host" , entity.task.details.entityUrl.host).increment()

        return entity.apply { continueProcessing = true }
    }
}