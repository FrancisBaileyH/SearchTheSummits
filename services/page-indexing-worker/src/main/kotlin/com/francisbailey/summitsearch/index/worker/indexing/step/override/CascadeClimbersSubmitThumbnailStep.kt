package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import org.springframework.stereotype.Component

/**
 * NoOp until we have a better system for generating thumbnails. Currently, CascadeClimbers has the same icon for
 * all og:image links.
 */
@Component
class CascadeClimbersSubmitThumbnailStep: Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        log.warn { "Skipping thumbnail for CascadeClimbers" }
        return entity
    }
}