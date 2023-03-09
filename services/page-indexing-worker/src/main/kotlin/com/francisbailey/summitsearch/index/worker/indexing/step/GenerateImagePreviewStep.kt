package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class GenerateImagePreviewStep: Step<ImmutableImage> {

    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        log.info { "Attempting to scale: ${entity.task.details.pageUrl}" }
        return entity.apply {
            payload = payload?.scaleToHeight(DEFAULT_HEIGHT)
            continueProcessing = true
        }
    }

    companion object {
        const val DEFAULT_HEIGHT = 200
    }
}