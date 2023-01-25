package com.francisbailey.summitsearch.index.worker.store

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class QueueAssignmentStore(
    meterRegistry: MeterRegistry
) {
    private val assignedQueues = mutableSetOf<String>()

    init {
        Gauge.builder("queue.assignment.count", assignedQueues) {
            it.size.toDouble()
        }.register(meterRegistry)
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
    }
}