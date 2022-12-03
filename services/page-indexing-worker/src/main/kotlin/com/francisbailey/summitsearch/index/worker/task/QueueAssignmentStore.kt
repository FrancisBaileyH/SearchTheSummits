package com.francisbailey.summitsearch.index.worker.task

import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class QueueAssignmentStore() {
    private val assignedQueues = mutableSetOf<String>()

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