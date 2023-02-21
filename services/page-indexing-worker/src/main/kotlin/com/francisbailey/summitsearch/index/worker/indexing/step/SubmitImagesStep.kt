package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.getCaptionedImages
import com.francisbailey.summitsearch.index.worker.extension.getWPCaptionedImages
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component
import java.time.ZoneOffset


@Component
class SubmitImagesStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        entity.payload?.document?.let {
            val images = it.getWPCaptionedImages() + it.getCaptionedImages()

            val imageDiscoveries = images.map { image ->
                ImageDiscovery(
                    description = image.caption,
                    source = image.imageSrc,
                    referencingURL = entity.task.details.pageUrl,
                    pageCreationDate = entity.payload?.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                )
            }.toSet()

            linkDiscoveryService.submitImages(entity.task, imageDiscoveries)
        }

        return entity
    }

}