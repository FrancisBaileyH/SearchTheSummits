package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.task.client.TaskQueueClient
import mu.KotlinLogging
import org.springframework.stereotype.Service


@Service
class IndexingQueueProvider(
    private val taskQueueClient: TaskQueueClient
){
    private val log = KotlinLogging.logger { }

    private val queues = mutableSetOf<String>()

    fun refreshQueues() {
        log.info { "Refreshing indexing task queues" }

        val refreshedQueues = taskQueueClient.listTaskQueues()

        log.info { "Found ${refreshedQueues.size} indexing task queues on last refresh" }

        synchronized(queues) {
            queues.clear()
            queues.addAll(refreshedQueues)
        }
    }

    fun getQueues(): Set<String> = synchronized(queues) { queues }
}



