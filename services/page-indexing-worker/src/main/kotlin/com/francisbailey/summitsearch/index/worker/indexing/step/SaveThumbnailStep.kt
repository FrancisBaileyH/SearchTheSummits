package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
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
        val context = entity.task.details.getContext<ImageTaskContext>()!!

        monitor.dependencyCircuitBreaker.executeCallable {
            val reference = monitor.meter.timer("imagestore.latency").recordCallable {
                imageWriterStore.save(entity.task.details.entityUrl, entity.payload!!.bytes(imageWriter), ImageStoreType.THUMBNAIL)
            }!!

            monitor.meter.timer("indexservice.latency").recordCallable {
                summitSearchIndexService.putThumbnails(
                    SummitSearchPutThumbnailRequest(
                        source = context.referencingURL,
                        dataStoreReferences = listOf(reference.toString())
                    )
                )
            }!!
        }
        monitor.meter.counter("indexservice.add.success", "host", entity.task.details.entityUrl.host)

        return entity.apply { continueProcessing = true }
    }


}