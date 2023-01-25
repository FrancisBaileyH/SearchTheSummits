package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.metadata.PageMetadataStore
import com.francisbailey.summitsearch.index.worker.extension.normalize
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.time.Instant
import java.util.UUID

class LinkDiscoveryTask(
    private val taskQueueClient: IndexingTaskQueueClient,
    private val pageMetadataStore: PageMetadataStore,
    private val associatedTask: IndexTask,
    private val documentFilterService: DocumentFilterService,
    private val meterRegistry: MeterRegistry,
    val discovery: String
): Runnable {

    private val log = KotlinLogging.logger {  }

    /**
     * Opportunity to do batch sends for the TaskQueueClient
     */
    override fun run() {
        try {
            val discoveryUrl = URL(discovery).normalize()
            val associatedTaskUrl = associatedTask.details.pageUrl

            if (discovery.length > MAX_LINK_SIZE) {
                log.warn { "Link: ${discovery.take(MAX_LINK_SIZE)} is too large to be processed" }
                return
            }

            if (discoveryUrl.host != associatedTaskUrl.host) {
                log.warn { "Link: $discoveryUrl is from external source. Skipping" }
                pageMetadataStore.saveDiscoveryMetadata(discoveryUrl.host)
                return
            }

            if (discoveryUrl == associatedTaskUrl) {
                log.warn { "Discovered URL is the same as task URL. Skipping" }
                return
            }

            if (documentFilterService.shouldFilter(discoveryUrl)) {
                log.warn { "Skipping $discoveryUrl as it matches a filter" }
                return
            }

            val metadata = meterRegistry.timer("$TASK_METRIC.metadata.get.latency").recordCallable {
                pageMetadataStore.getMetadata(discoveryUrl)
            }

            if (metadata == null || metadata.canRefresh(associatedTask.details.refreshDuration())) {
                log.info { "New link discovery: $discoveryUrl as part of: ${associatedTask.details.id}" }
                taskQueueClient.addTask(
                    IndexTask(
                        source = associatedTask.source,
                        details = IndexTaskDetails(
                            id = UUID.randomUUID().toString(),
                            taskRunId = associatedTask.details.taskRunId,
                            pageUrl = discoveryUrl,
                            submitTime = Instant.now().toEpochMilli(),
                            taskType = associatedTask.details.taskType,
                            refreshIntervalSeconds = associatedTask.details.refreshIntervalSeconds
                        )
                    )
                )
                meterRegistry.counter("$TASK_METRIC.newlink", "queue", associatedTask.source).increment()
                meterRegistry.timer("$TASK_METRIC.metadata.put.latency").recordCallable {
                    pageMetadataStore.saveMetadata(associatedTask.details.taskRunId, discoveryUrl)
                }
                log.info { "Successfully processed discovery" }
            }
        } catch (e: Exception) {
            when (e) {
                is MalformedURLException,
                is URISyntaxException ->
                    log.debug { "Bad URL for discovery: $discovery" }
                else -> {
                    meterRegistry.counter("$TASK_METRIC.exception", "type", e.javaClass.simpleName).increment()
                    log.error(e) { "Unable to process link: $discovery" }
                }

            }
        }
    }

    companion object {
        const val MAX_LINK_SIZE = 255
        const val TASK_METRIC = "task.linkdiscovery"
    }

}