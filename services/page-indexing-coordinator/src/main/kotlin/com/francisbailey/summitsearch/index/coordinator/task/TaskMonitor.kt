package com.francisbailey.summitsearch.index.coordinator.task

import com.francisbailey.summitsearch.index.coordinator.sources.IndexSource
import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class TaskMonitor(
    private val taskStore: TaskStore,
    private val emptyQueueMonitorDuration: Duration,
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val meter: MeterRegistry,
    private val clock: Clock
) {
    private val log = KotlinLogging.logger { }

    fun getActiveTasks(): List<Task> {
        return taskStore.getTasks().filter {
            it.status == TaskStatus.RUNNING
        }
    }

    fun hasTaskForSource(source: IndexSource): Boolean {
        return taskStore.getTask(source.host) != null
    }

    fun enqueueTaskForSource(source: IndexSource) {
        val task = Task(
            id = UUID.randomUUID().toString(),
            host = source.host,
            queueUrl = source.queueUrl,
            status = TaskStatus.PENDING,
            monitorTimestamp = null,
            seeds = source.seeds,
            refreshInterval = source.documentTtl
        )

        taskStore.save(task)
        meter.counter("$service.enqueued", "host", task.host).increment()
    }

    /**
     * Tasks start in the PENDING state and transition to RUNNING once all
     * seed URLs have been added to the task queue
     *
     * The task will stay in the RUNNING state until the queue is empty at which point
     * the queue will be monitored up to "emptyQueueMonitorDuration" before it is marked
     * as COMPLETED.
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun monitorTasks() {
        val activeTasks = taskStore.getTasks()

        activeTasks.forEach {
            val tags = arrayOf("status", it.status.name, "host", it.host)
            log.info { "Task: $it is in state: ${it.status}" }
            meter.counter("$service.task-count", *tags).increment()

            when(it.status) {
                TaskStatus.PENDING -> {
                    seedIndexTaskQueue(it)
                    taskStore.save(it.apply {
                        status = TaskStatus.RUNNING
                    })
                }
                TaskStatus.RUNNING -> {
                    if (hasIndexTasksInQueue(it)) {
                        it.monitorTimestamp = null
                    } else {
                        it.monitorTimestamp = it.monitorTimestamp ?: clock.instant().toEpochMilli()
                    }

                    if (isTaskComplete(it)) {
                        it.status = TaskStatus.COMPLETED
                    }

                    taskStore.save(it)
                }
                TaskStatus.COMPLETED -> {
                    taskStore.delete(it)
                }
            }
            log.info { "Task: $it is now in state: ${it.status}" }
        }
    }


    private fun isTaskComplete(task: Task): Boolean {
        val currentTime = clock.instant()
        val monitorTimestamp = task.monitorTimestamp

        if (monitorTimestamp == null) {
            return false
        }

        val emptyQueueMonitorThreshold = Instant
            .ofEpochMilli(monitorTimestamp)
            .plus(emptyQueueMonitorDuration)

        return emptyQueueMonitorThreshold < currentTime
    }

    private fun hasIndexTasksInQueue(task: Task): Boolean {
        val taskCount = indexingTaskQueueClient.getTaskCount(task.queueUrl)
        return taskCount > 0
    }

    private fun seedIndexTaskQueue(task: Task) {
        val taskRunId = UUID.randomUUID().toString()

        log.info { "Generating seed tasks for: ${task.host} with: ${task.seeds}" }
        log.info { "Task run id: $taskRunId" }

        val seedTasks = task.seeds.map {
            IndexTask(
                source = task.queueUrl,
                details = IndexTaskDetails(
                    entityUrl = URL(it),
                    entityTtl = task.refreshInterval,
                    taskType = IndexTaskType.HTML,
                    submitTime = clock.instant().toEpochMilli(),
                    taskRunId = taskRunId,
                    id = UUID.randomUUID().toString()
                )
            )
        }

        seedTasks.chunked(IndexingTaskQueueClient.MAX_MESSAGE_BATCH_SIZE).forEach {
            log.info { "Sending batch index tasks of size ${it.size}" }
            indexingTaskQueueClient.addTasks(it)
        }
    }

    companion object {
        const val service = "task-monitor"
    }
}