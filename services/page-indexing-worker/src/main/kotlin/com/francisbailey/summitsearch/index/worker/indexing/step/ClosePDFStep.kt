package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component

@Component
class ClosePDFStep: Step<PDDocument> {
    override fun process(entity: PipelineItem<PDDocument>, monitor: PipelineMonitor): PipelineItem<PDDocument> {
        return entity.apply { payload?.close() }
    }
}