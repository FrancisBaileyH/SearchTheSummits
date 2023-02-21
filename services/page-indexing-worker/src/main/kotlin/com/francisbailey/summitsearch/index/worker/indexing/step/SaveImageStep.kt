package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchImagePutRequest
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
        return try {
            val context = entity.task.details.getContext<ImageTaskContext>()!!

            monitor.dependencyCircuitBreaker.executeCallable {
                val reference = monitor.meter.timer("$metricPrefix.imagestore.latency").recordCallable {
                    imageWriterStore.save(entity.task.details.pageUrl, entity.payload!!.bytes(imageWriter), ImageStoreType.STANDARD)
                }!!

                monitor.meter.timer("$metricPrefix.imageindexservice.latency").recordCallable {
                    imageIndexService.indexImage(SummitSearchImagePutRequest(
                        source = entity.task.details.pageUrl,
                        dataStoreReference = reference.toString(),
                        description = context.description,
                        referencingDocument = context.referencingURL,
                        referencingDocumentDate = context.pageCreationDate
                    ))
                }!!
            }
            monitor.meter.counter("$metricPrefix.imageindexservice.add.success", "host", entity.task.details.pageUrl.host)

            entity
        } catch (e: Exception) {
            log.error(e) { "Failed to save image from: ${entity.task.details.pageUrl}" }
            entity
        }
    }

}