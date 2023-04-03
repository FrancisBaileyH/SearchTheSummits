package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.DocumentExistsRequest
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class ThumbnailValidationStep(
    private val documentIndexService: DocumentIndexService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        val referencingUrl = entity.task.details.getContext<ImageTaskContext>()?.referencingURL!!
        val request = DocumentExistsRequest(referencingUrl)

        monitor.dependencyCircuitBreaker.executeCallable {
            if (!documentIndexService.pageExists(request)) {
                log.warn { "Thumbnail has no associated document: $referencingUrl in the index. Skipping." }
                entity.continueProcessing = false
            } else {
                entity.continueProcessing = true
            }
        }

        return entity
    }
}