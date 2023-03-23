package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.ImageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.RedirectedEntityException
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.sksamuel.scrimage.ImmutableImage
import org.springframework.stereotype.Component
import java.net.MalformedURLException
import java.net.URL


@Component
class FetchImageStep(
    private val imageCrawlerService: ImageCrawlerService,
    private val linkDiscoveryService: LinkDiscoveryService
): Step<ImmutableImage> {
    override fun process(entity: PipelineItem<ImmutableImage>, monitor: PipelineMonitor): PipelineItem<ImmutableImage> {
        try {
            val image = monitor.sourceCircuitBreaker.executeCallable {
                monitor.meter.timer("image.latency", "host", entity.task.details.entityUrl.host).recordCallable {
                    imageCrawlerService.get(entity.task.details.entityUrl)
                }!!
            }

            return entity.apply {
                payload = image
                continueProcessing = true
            }
        } catch (e: RedirectedEntityException) {
            e.location?.let {
                processRedirect(it, entity.task)
            }

            throw e
        }
    }

    /**
     * Support a limited redirect from http to https and vice versa
     */
    private fun processRedirect(redirectString: String, task: IndexTask) {
        try {
            val redirectUrl = URL(redirectString)
            val imageContext = task.details.getContext<ImageTaskContext>()!!

            if (URLProperties.all { it.invoke(redirectUrl) == it.invoke(task.details.entityUrl)}) {
                linkDiscoveryService.submitImages(task, setOf(ImageDiscovery(
                    description = imageContext.description,
                    pageCreationDate = imageContext.pageCreationDate,
                    referencingURL = imageContext.referencingURL,
                    source = redirectUrl.toString(),
                    type = when(task.details.taskType) {
                        IndexTaskType.THUMBNAIL -> ImageDiscoveryType.THUMBNAIL
                        else -> ImageDiscoveryType.STANDARD
                    }
                )))
            }

        } catch (e: MalformedURLException) {
            throw NonRetryableEntityException("Unable to process redirect to: $redirectString")
        }
    }

    companion object {
        val URLProperties = listOf(
            URL::getHost,
            URL::getPath,
            URL::getQuery
        )
    }
}