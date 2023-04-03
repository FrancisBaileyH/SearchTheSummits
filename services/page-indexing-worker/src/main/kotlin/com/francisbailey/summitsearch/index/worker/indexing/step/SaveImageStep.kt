package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.extension.stripQueryAndFragment
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.ImagePutRequest
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImageWriter
import org.springframework.stereotype.Component


@Component
class SaveImageStep(
    private val imageWriterStore: ImageWriterStore,
    private val imageIndexService: ImageIndexService,
    private val imageWriter: ImageWriter
): Step<ImmutableImage> {

    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        val context = entity.task.details.getContext<ImageTaskContext>()!!
        val normalizedUrl = entity.task.details.entityUrl.stripQueryAndFragment()

        monitor.dependencyCircuitBreaker.executeCallable {
            val reference = monitor.meter.timer("imagestore.latency").recordCallable {
                imageWriterStore.save(normalizedUrl, entity.payload!!.bytes(imageWriter), ImageStoreType.STANDARD)
            }!!

            monitor.meter.timer("imageindexservice.latency").recordCallable {
                imageIndexService.indexImage(ImagePutRequest(
                    source = entity.task.details.entityUrl,
                    normalizedSource = entity.task.details.entityUrl.stripQueryAndFragment(),
                    dataStoreReference = reference.toString(),
                    description = context.description,
                    referencingDocument = context.referencingURL,
                    referencingDocumentDate = context.pageCreationDate,
                    heightPx = entity.payload!!.height,
                    widthPx = entity.payload!!.width
                ))
            }!!
        }
        monitor.meter.counter("imageindexservice.add.success", "host", entity.task.details.entityUrl.host)

        return entity.apply { continueProcessing = true }
    }

}