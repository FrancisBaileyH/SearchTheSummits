package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractor
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component
import java.time.ZoneOffset


@Component
class SubmitImagesStep(
    private val linkDiscoveryService: LinkDiscoveryService,
    private val imageContentExtractor: ContentExtractor<List<CaptionedImage>>
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        entity.payload?.document?.let {
            val images = imageContentExtractor.extract(entity.task.details.entityUrl, it)

            val imageDiscoveries = images.map { image ->
                ImageDiscovery(
                    description = image.caption,
                    source = image.imageSrc,
                    referencingURL = entity.task.details.entityUrl,
                    referencingTitle = it.title(),
                    type = ImageDiscoveryType.STANDARD,
                    pageCreationDate = entity.payload?.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                )
            }.toSet()
            log.info { "Found: ${imageDiscoveries.size} candidate images from: ${entity.task.details.entityUrl}" }
            linkDiscoveryService.submitImages(entity.task, imageDiscoveries)
        }

        return entity.apply { continueProcessing = true }
    }

}