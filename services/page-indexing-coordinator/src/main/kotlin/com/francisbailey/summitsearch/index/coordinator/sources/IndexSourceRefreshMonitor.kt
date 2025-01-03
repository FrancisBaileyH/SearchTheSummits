package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import com.francisbailey.summitsearch.services.common.RegionConfig
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Clock
import java.util.concurrent.TimeUnit

@Service
class IndexSourceRefreshMonitor(
    private val meterRegistry: MeterRegistry,
    regionConfig: RegionConfig,
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val indexSourceRepository: IndexSourceStore,
    private val taskMonitor: TaskMonitor,
    private val clock: Clock
) {
    private val log = KotlinLogging.logger { }

    private val queuePrefix = when {
        regionConfig.isProd -> "sts-index-queue-"
        else -> "sts-index-queue-test-"
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    fun checkSources() = try {
        val sources = indexSourceRepository.getRefreshableSources()

        log.info { "Found ${sources.size} refreshable sources" }
        meterRegistry.counter("index-source-refresh-monitor.source-count").increment()

        sources.forEach {
            if (it.queueUrl.isBlank()) {
                val queueName = generateQueueName(it.host)

                log.info { "Queue: $queueName not found. Generating now." }
                indexSourceRepository.save(it.apply {
                    queueUrl = indexingTaskQueueClient.createQueue(queueName)
                })
            }

            if (!taskMonitor.hasTaskForSource(it)) {
                log.info { "No active task found for source: $it. Enqueueing now." }
                taskMonitor.enqueueTaskForSource(it)
            }

            indexSourceRepository.save(it.apply {
                 nextUpdate = clock
                     .instant()
                     .plusSeconds(it.refreshIntervalSeconds)
                     .toEpochMilli()
            })

            log.info { "Successfully processed: $it" }
            meterRegistry.counter("index-source-refresh-monitor.enqueued").increment()
        }
    } catch (e: Exception) {
        log.error(e) { "Failed to check sources" }
     }

    private fun generateQueueName(host: String): String {
        return "$queuePrefix${host.replace(".", "-")}"
    }
}