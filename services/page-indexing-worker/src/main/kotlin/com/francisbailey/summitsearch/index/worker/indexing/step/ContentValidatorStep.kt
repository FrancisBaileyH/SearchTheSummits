package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import org.springframework.stereotype.Component

/**
 * Just a pass through step for now so that we can override this
 * with other hosts
 */
@Component
class ContentValidatorStep: Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        return entity.apply { continueProcessing = true }
    }
}