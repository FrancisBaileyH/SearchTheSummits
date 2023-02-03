package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class PeakBaggerContentValidatorStep: Step<Document> {
    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        val page = entity.task.details.pageUrl

        if (page.path.startsWith("/climber/ascent.aspx")) {
            val reportContentTitle = entity.payload?.selectFirst("h2:contains(Ascent Trip Report)")
            val reportContent = reportContentTitle?.parent()?.text() ?: ""

            if (reportContent.length < MINIMUM_REPORT_LENGTH) {
                monitor.meter.counter("Pipeline.contentvalidator.failed", "host", page.host).increment()
                log.warn { "Skipped page: $page, because it does not have minimum content lenght of: $MINIMUM_REPORT_LENGTH" }
                entity.continueProcessing = false
            }
        }

        return entity
    }

    companion object {
        const val MINIMUM_REPORT_LENGTH = 400
    }
}