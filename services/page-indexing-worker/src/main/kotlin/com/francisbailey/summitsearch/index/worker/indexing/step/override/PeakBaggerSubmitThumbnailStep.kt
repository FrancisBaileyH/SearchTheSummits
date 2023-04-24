package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.extension.src
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component


@Component
class PeakBaggerSubmitThumbnailStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val imageSrc = entity.payload?.let {
            val candidateImages = it.document.select("img[src~=(?i)\\.(png|jpe?g)]")

            candidateImages.map { image ->
                image.src()
            }.firstOrNull { src ->
                src?.contains("peakbaggerblobs.blob.core.windows.net") ?: false
            }
        }

        imageSrc?.let {
            linkDiscoveryService.submitImages(entity.task, setOf(
                ImageDiscovery(
                    source = it,
                    referencingURL = entity.task.details.entityUrl,
                    description = "",
                    type = ImageDiscoveryType.THUMBNAIL
                )
            ))
        }

        return entity.apply { continueProcessing = true }
    }

}