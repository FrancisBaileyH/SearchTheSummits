package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.stripQueryAndFragment
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

/**
 * It's computationally expensive for both the indexing worker and the image host
 * to fetch and scale the images. Since we're reindexing the pages often we should
 * skip re-fetching and indexing images if they already exist in the store. The one caveat
 * to this approach is that image descriptions will not be added.
 */
@Component
class CheckImageExistsStep(
    private val imageWriterStore: ImageWriterStore
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        val normalizedUrl = entity.task.details.pageUrl.stripQueryAndFragment()

        val exists = monitor.dependencyCircuitBreaker.executeCallable {
            log.info { "Checking if $normalizedUrl exists in image store" }
            imageWriterStore.exists(normalizedUrl, ImageStoreType.STANDARD)
        }

        if (exists) {
            monitor.meter.counter("image.skipped" , "reason", "exists").increment()
            log.warn { "File: $normalizedUrl exists in image store. Skipping" }
            entity.continueProcessing = false
        } else {
            entity.continueProcessing = true
        }

        return entity
    }
}