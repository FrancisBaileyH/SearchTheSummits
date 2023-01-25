package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component


@Component
class SubmitLinksStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<Document> {

    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        val organicLinks = entity.payload?.body()?.getLinks() ?: emptyList()
        linkDiscoveryService.submitDiscoveries(entity.task, organicLinks)
        monitor.meter.counter("$metricPrefix.links.discovered").increment(organicLinks.size.toDouble())

        return entity
    }
}