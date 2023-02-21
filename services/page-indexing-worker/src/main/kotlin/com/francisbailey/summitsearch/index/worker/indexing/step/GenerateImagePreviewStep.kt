package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class GenerateImagePreviewStep: Step<ImmutableImage> {

    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        return try {
            log.info { "Attempting to scale: ${entity.task.details.pageUrl}" }
            entity.apply {
                payload = payload?.scaleToHeight(DEFAULT_HEIGHT)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to scale image: ${entity.task.details.pageUrl}" }
            entity.apply { continueProcessing = false }
        }
    }

    companion object {
        const val DEFAULT_HEIGHT = 200
    }
}