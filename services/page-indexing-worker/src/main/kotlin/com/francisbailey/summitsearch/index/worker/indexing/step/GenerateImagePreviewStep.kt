package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class GenerateImagePreviewStep: Step<ImmutableImage> {

    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        log.info { "Attempting to scale: ${entity.task.details.entityUrl}" }

        if (entity.payload!!.height < MIN_HEIGHT || entity.payload!!.width < MIN_WIDTH) {
            log.warn { "Image: ${entity.task.details.entityUrl} size is too small. Skipping" }
            return entity.apply { this.continueProcessing = false }
        }

        return entity.apply {
            payload = payload?.scaleToHeight(DEFAULT_HEIGHT)

            continueProcessing = true
        }
    }

    companion object {
        const val DEFAULT_HEIGHT = 200
        const val MIN_WIDTH = 150
        const val MIN_HEIGHT = DEFAULT_HEIGHT
    }
}