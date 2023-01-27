package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchImagePutRequest
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import org.springframework.stereotype.Component

@Component
class SaveThumbnailStep(
    private val imageWriterStore: ImageWriterStore,
    private val imageWriter: ImageWriter,
    private val imageIndexService: ImageIndexService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        return try {
            val path = ImageWriterStore.buildPathFromUrl(entity.task.details.pageUrl)
            val context = entity.task.details.getContext<ImageTaskContext>()!!

            monitor.dependencyCircuitBreaker.executeCallable {
                val reference = monitor.meter.timer("$metricPrefix.imagestore.latency").recordCallable {
                    imageWriterStore.save(path, entity.payload!!.bytes(imageWriter))
                }!!

                monitor.meter.timer("$metricPrefix.imageindex.latency").recordCallable {
                    imageIndexService.indexThumbnail(
                        SummitSearchImagePutRequest(
                            source = entity.task.details.pageUrl,
                            referencingDocument = context.referencingURL,
                            description = context.description,
                            dataStoreReference = reference.toString()
                        )
                    )
                }!!
            }

            entity
        } catch (e: Exception) {
            log.error(e) { "Failed to save thumbnail from: ${entity.task.details.pageUrl}" }
            entity
        }
    }


}