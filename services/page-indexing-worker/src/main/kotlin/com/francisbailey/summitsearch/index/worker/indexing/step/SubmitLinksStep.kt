package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component


@Component
class SubmitLinksStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val organicLinks = entity.payload?.document?.body()?.getLinks() ?: emptyList()

        val discoveries = organicLinks.map {
            val type = when {
                it.endsWith("pdf", ignoreCase = true) -> IndexTaskType.PDF
                else -> IndexTaskType.HTML
            }

            Discovery(type, it)
        }

        linkDiscoveryService.submitDiscoveries(entity.task, discoveries)
        monitor.meter.counter("links.discovered").increment(discoveries.size.toDouble())

        return entity.apply { continueProcessing = true }
    }
}