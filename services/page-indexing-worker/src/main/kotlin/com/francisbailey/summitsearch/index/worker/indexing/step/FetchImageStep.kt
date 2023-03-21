package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.crawler.ImageCrawlerService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component

/**
 * Do not support redirects for now. If we decide to in the future, we'll need to leverage PageMetadataStore
 * to avoid a redirect cycle.
 */
@Component
class FetchImageStep(
    private val imageCrawlerService: ImageCrawlerService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        val image = monitor.sourceCircuitBreaker.executeCallable {
            monitor.meter.timer("image.latency", "host", entity.task.details.entityUrl.host).recordCallable {
                imageCrawlerService.get(entity.task.details.entityUrl)
            }!!
        }

        return entity.apply {
            payload = image
            continueProcessing = true
        }
    }
}