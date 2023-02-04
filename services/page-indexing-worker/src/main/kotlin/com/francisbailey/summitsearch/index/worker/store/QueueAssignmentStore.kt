package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class QueueAssignmentStore(
    private val meterRegistry: MeterRegistry,
    private val indexingTaskQueueClient: IndexingTaskQueueClient
) {
    private val assignedQueues = mutableSetOf<String>()

    private val taskCountGauge: MultiGauge

    private val taskCountTracker = mutableMapOf<String, Long>()

    init {
        Gauge
            .builder("queue.assignment.count", assignedQueues) {
                it.size.toDouble()
            }
            .register(meterRegistry)

        taskCountGauge = MultiGauge
            .builder("queue.assignment.task.count")
            .register(meterRegistry)
    }

    private val log = KotlinLogging.logger {  }

    fun assign(queues: Set<String>) = synchronized(this) {
        log.info { "Assigning ${queues.size} queues" }
        assignedQueues.addAll(queues)
    }

    fun getAssignments(): Set<String> = synchronized(this) {
        assignedQueues
    }

    fun clearAssignments() = synchronized(this) {
        log.info { "Clearing all assignments" }
        assignedQueues.clear()
        taskCountGauge.register(emptyList())
        taskCountTracker.clear()
    }

    /**
     * See @BackgroundSchedulerConfiguration
     */
    fun updateTaskCount() {
        assignedQueues.forEach {
            try {
                taskCountTracker[it] = indexingTaskQueueClient.getTaskCount(it)
            } catch (e: Exception) {
                meterRegistry.counter("queue.assignment.task.tracker.exception", "type", e::class.simpleName)
                log.error(e) { "Failed to retrieve task count" }
            }
        }

        taskCountGauge.register(taskCountTracker.map {
            MultiGauge.Row.of(Tags.of("queue", it.key)) {
                it.value
            }
        })
    }
}