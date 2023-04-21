package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.getFigCaptionedImages
import com.francisbailey.summitsearch.index.worker.extension.getOGImage
import com.francisbailey.summitsearch.index.worker.extension.getWPCaptionedImages
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component


@Component
class SubmitThumbnailStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val document = entity.payload?.document
        val thumbnailCandidate = document?.getOGImage() ?:
        document?.getFigCaptionedImages()?.firstOrNull() ?:
        document?.getWPCaptionedImages()?.firstOrNull()

        thumbnailCandidate?.let {
            monitor.dependencyCircuitBreaker.executeCallable {
                val details = entity.task.details
                log.info { "Found thumbnail: ${thumbnailCandidate.imageSrc} on ${details.entityUrl}" }

                linkDiscoveryService.submitImages(entity.task, setOf(
                    ImageDiscovery(
                        description = it.caption,
                        source = it.imageSrc,
                        referencingURL = entity.task.details.entityUrl,
                        type = ImageDiscoveryType.THUMBNAIL,
                        pageCreationDate = null
                    )
                ))
            }
        }

        return entity.apply { continueProcessing = true }
    }
}