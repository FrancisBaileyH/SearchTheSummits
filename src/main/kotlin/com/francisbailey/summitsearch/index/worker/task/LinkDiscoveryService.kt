package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.client.TaskStore
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
    private val taskStore: TaskStore
) {
    private val log = KotlinLogging.logger {  }


    fun submitDiscoveries(associatedTask: IndexTask, discoveries: List<String>) {
        log.info { "Processing: ${discoveries.size} link discoveries" }
        discoveries.filterNot {
            it.isEmpty()
        }.toSet().forEach {
            linkDiscoveryTaskExecutor.execute(LinkDiscoveryTask(
                taskQueueClient = taskQueueClient,
                taskStore = taskStore,
                associatedTask = associatedTask,
                discovery = it
            ))
        }
    }
}