package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import org.springframework.stereotype.Component

@Component
class MountainProjectContentValidatorStep: Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val page = entity.task.details.entityUrl
        entity.continueProcessing = true

        if (page.path.startsWith("/route")) {
            val descriptionTable = entity.payload?.document?.selectFirst("table.description-details")
            val isAlpineRoute = descriptionTable
                ?.select("tr")
                ?.any {
                    val text = it.text()
                    text.contains("Type:") && text.contains("Alpine")
                }

            entity.continueProcessing = isAlpineRoute ?: false
        }

        return entity
    }
}