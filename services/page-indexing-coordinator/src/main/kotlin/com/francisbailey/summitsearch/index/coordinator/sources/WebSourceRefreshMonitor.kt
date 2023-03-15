package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

@Service
class WebSourceRefreshMonitor(
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val indexSourceRepository: IndexSourceStore,
    private val taskMonitor: TaskMonitor
) {

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    fun checkSources() {
        val sources = indexSourceRepository.getRefreshableSources()

        sources.forEach {
            val queueName = "IndexingQueue-${it.host!!.replace(".", "-")}"

            if (indexingTaskQueueClient.queueExists(queueName)) {
                indexSourceRepository.save(it.apply {
                    queueUrl = indexingTaskQueueClient.createQueue(queueName)
                })
            }

            if (!taskMonitor.hasActiveTaskForSource(it)) {
                taskMonitor.enqueueTaskForSource(it)
            }

            indexSourceRepository.save(it.apply {
                lastUpdate = Instant.now().toEpochMilli()
            })
        }
    }
}