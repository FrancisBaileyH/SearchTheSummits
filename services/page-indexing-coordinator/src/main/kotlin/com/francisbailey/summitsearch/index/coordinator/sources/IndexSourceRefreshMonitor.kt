package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import com.francisbailey.summitsearch.services.common.RegionConfig
import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class IndexSourceRefreshMonitor(
    private val meterRegistry: MeterRegistry,
    private val regionConfig: RegionConfig,
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val indexSourceRepository: IndexSourceStore,
    private val taskMonitor: TaskMonitor
) {
    private val log = KotlinLogging.logger { }

    private val queuePrefix = when {
        regionConfig.isProd -> "IndexingQueue-"
        else -> "IndexingQueue-Test-"
    }

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    fun checkSources() {
        val sources = indexSourceRepository.getRefreshableSources()

        log.info { "Found ${sources.size} refreshable sources" }
        meterRegistry.counter("index-source-refresh-monitor.source-count").increment()

        sources.forEach {
            val queueName = generateQueueName(it.host)

            if (!indexingTaskQueueClient.queueExists(queueName)) {
                log.info { "Queue: $queueName not found. Generating now." }
                indexSourceRepository.save(it.apply {
                    queueUrl = indexingTaskQueueClient.createQueue(queueName)
                })
            }

            if (!taskMonitor.hasActiveTaskForSource(it)) {
                log.info { "No active task found for source: $it. Enqueueing now." }
                taskMonitor.enqueueTaskForSource(it)
            }

            indexSourceRepository.save(it.apply {
                 nextUpdate = Instant
                     .now()
                     .plusSeconds(it.refreshIntervalSeconds)
                     .toEpochMilli()
            })

            log.info { "Successfully processed: $it" }
            meterRegistry.counter("index-source-refresh-monitor.enqueued").increment()
        }
    }

    private fun generateQueueName(host: String): String {
        return "$queuePrefix${host.replace(".", "-")}"
    }
}