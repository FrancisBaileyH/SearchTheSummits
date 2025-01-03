package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.store.PageMetadataStore
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.util.concurrent.Executor

/**
 * Any time a link is discovered on a web page, we'll run a task
 * to process it. If the link is from the same origin and it has not
 * already been processed, we will submit it to the IndexingTaskQueue
 */
@Service
class LinkDiscoveryService(
    private val linkDiscoveryTaskExecutor: Executor,
    private val taskQueueClient: IndexingTaskQueueClient,
    private val pageMetadataStore: PageMetadataStore,
    private val linkDiscoveryFilterService: DocumentFilterService,
    private val meterRegistry: MeterRegistry
) {
    private val log = KotlinLogging.logger {  }


    fun submitDiscoveries(associatedTask: IndexTask, discoveries: List<Discovery>) {
        log.info { "Processing: ${discoveries.size} link discoveries" }
        discoveries.filterNot {
            it.source.isEmpty()
        }.toSet().forEach {
            linkDiscoveryTaskExecutor.execute(LinkDiscoveryTask(
                taskQueueClient = taskQueueClient,
                pageMetadataStore = pageMetadataStore,
                associatedTask = associatedTask,
                documentFilterService = linkDiscoveryFilterService,
                meterRegistry = meterRegistry,
                discovery = it
            ))
        }
    }

    fun submitImages(associatedTask: IndexTask, discoveries: Set<ImageDiscovery>) {
        log.info { "Processing ${discoveries.size} image discoveries" }
        linkDiscoveryTaskExecutor.execute(ImageDiscoveryTask(
            taskQueueClient = taskQueueClient,
            discoveries = discoveries,
            associatedTask = associatedTask
        ))
    }
}