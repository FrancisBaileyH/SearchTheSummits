package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import org.springframework.stereotype.Component

@Component
class IndexFacebookPostStep(
    private val summitSearchIndexService: DocumentIndexService
): Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val document = entity.payload!!.document
        val seoDescription = document.selectFirst("meta[property=og:description]")?.attr("content") ?: ""
        val imageCaption = document.selectFirst("meta[property=og:image:alt]")?.attr("content") ?: ""
        val title = document.title()

        monitor.meter.timer("indexservice.add.latency").recordCallable {
            monitor.dependencyCircuitBreaker.executeCallable {
                summitSearchIndexService.indexContent(
                    DocumentPutRequest(
                        source = entity.task.details.entityUrl,
                        title = title,
                        rawTextContent = "",
                        paragraphContent = imageCaption,
                        seoDescription = seoDescription,
                        pageCreationDate = entity.payload!!.pageCreationDate
                    )
                )
            }
        }

        log.info { "Successfully completed indexing facebook post: ${entity.task.source} with ${entity.task.details.entityUrl}" }
        monitor.meter.counter("indexservice.add.success", "host" , entity.task.details.entityUrl.host).increment()

        return entity.apply { continueProcessing = true }
    }

}