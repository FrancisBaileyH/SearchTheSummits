package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.SummitSearchExistsRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class ThumbnailValidationStep(
    private val summitSearchIndexService: SummitSearchIndexService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        try {
            val referencingUrl = entity.task.details.getContext<ImageTaskContext>()?.referencingURL!!
            val request = SummitSearchExistsRequest(referencingUrl)

            monitor.dependencyCircuitBreaker.executeCallable {
                if (!summitSearchIndexService.pageExists(request)) {
                    log.warn { "Thumbnail has no associated document: $referencingUrl in the index. Skipping." }
                    entity.continueProcessing = false
                }
            }

        } catch (e: Exception) {
            log.error(e) { "Failed to check if referencing URL is indexed already" }
            entity.continueProcessing = false
        }

        return entity
    }
}