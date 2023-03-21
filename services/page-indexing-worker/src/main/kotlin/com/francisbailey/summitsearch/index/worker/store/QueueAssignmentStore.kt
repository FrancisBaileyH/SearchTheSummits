package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.services.common.MonotonicClock
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import kotlin.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Service
class QueueAssignmentStore(
    private val meterRegistry: MeterRegistry,
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val clock: MonotonicClock
) {
    private val assignedQueues = mutableSetOf<String>()

    private val taskCountGauge: MultiGauge

    private val taskCountTracker = mutableMapOf<String, Long>()

    private var lastKeepAliveTime: MonotonicClock.TimeMark? = null

    init {
        Gauge.builder("queue.assignment.count", assignedQueues) { it.size.toDouble() }
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

    fun updateAssignmentKeepAliveState() {
        lastKeepAliveTime = clock.now()
    }

    /**
     * If we do not receive a keep alive call then we'll clear our
     * assignments as this indicates that we've lost connection with
     * the coordinator
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    fun monitorAssignmentKeepAliveState() {
        val keepAliveTime = lastKeepAliveTime

        if (keepAliveTime == null || clock.timeSince(keepAliveTime) > KEEP_ALIVE_DURATION) {
            if (assignedQueues.isNotEmpty()) {
                log.warn { "No keep alive refresh in the last: ${KEEP_ALIVE_DURATION.inWholeSeconds} seconds." }
                log.warn { "Clearing all assignments" }

                assignedQueues.clear()
            }
        }
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
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

    companion object {
        private val KEEP_ALIVE_DURATION: Duration = 10.seconds
    }
}