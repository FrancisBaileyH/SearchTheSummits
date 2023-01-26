package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

@Component
class GenerateThumbnailStep: Step<ImmutableImage> {

    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        return try {
            entity.apply {
                payload = payload?.scaleToHeight(DEFAULT_HEIGHT)
            }
        } catch (e: Exception) {
            entity.apply { continueProcessing = false }
        }
    }

    companion object {
        const val DEFAULT_HEIGHT = 92
    }
}