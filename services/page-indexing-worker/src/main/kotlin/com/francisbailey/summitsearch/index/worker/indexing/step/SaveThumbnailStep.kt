package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchPutThumbnailRequest
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import org.springframework.stereotype.Component

@Component
class SaveThumbnailStep(
    private val imageWriterStore: ImageWriterStore,
    private val imageWriter: ImageWriter,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        return try {
            val context = entity.task.details.getContext<ImageTaskContext>()!!

            monitor.dependencyCircuitBreaker.executeCallable {
                val reference = monitor.meter.timer("$metricPrefix.imagestore.latency").recordCallable {
                    imageWriterStore.save(entity.task.details.pageUrl, entity.payload!!.bytes(imageWriter))
                }!!

                monitor.meter.timer("$metricPrefix.indexservice.latency").recordCallable {
                    summitSearchIndexService.putThumbnails(
                        SummitSearchPutThumbnailRequest(
                            source = context.referencingURL,
                            dataStoreReferences = listOf(reference.toString())
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