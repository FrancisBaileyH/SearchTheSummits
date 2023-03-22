package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.extension.getLinks
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URISyntaxException

/**
 * Cookies are broken on this site and so
 * all links have &sid=<n> attached as a result. We can't
 * generically account for this, so let's just strip it in the mean time.
 */
@Component
class SkiSicknessSubmitLinksStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val organicLinks = entity.payload?.document?.body()?.getLinks() ?: emptyList()

        val discoveries = organicLinks.mapNotNull {
            try {
                val link = URI(it)
                val params = link.query?.split("&")

                val sanitizedParams = params?.filterNot { param ->
                    param.startsWith("sid")
                }?.joinToString("&")

                val query = sanitizedParams?.ifBlank {
                    null
                }

                val sanitizedUri = URI(
                    link.scheme,
                    link.userInfo,
                    link.host,
                    link.port,
                    link.path,
                    query,
                    link.fragment
                )

                Discovery(IndexTaskType.HTML, sanitizedUri.toString())
            } catch (e: URISyntaxException) {
                null
            }
        }

        linkDiscoveryService.submitDiscoveries(entity.task, discoveries)
        monitor.meter.counter("links.discovered").increment(discoveries.size.toDouble())

        return entity.apply { continueProcessing = true }
    }


}