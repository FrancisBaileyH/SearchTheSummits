package com.francisbailey.summitsearch.index.coordinator.task

import com.francisbailey.summitsearch.index.coordinator.*
import com.francisbailey.summitsearch.taskclient.IndexTask
import com.francisbailey.summitsearch.taskclient.IndexTaskDetails
import com.francisbailey.summitsearch.taskclient.IndexTaskType
import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.net.URL
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit


@Service
class TaskMonitor(
    private val taskStore: TaskStore,
    private val taskMonitorConfiguration: TaskMonitorConfiguration,
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val meter: MeterRegistry
) {
    private val log = KotlinLogging.logger { }

    private val activeTaskCache = mutableSetOf<Task>()

    fun getActiveTasks(): List<Task> = synchronized(this) {
        return activeTaskCache.toList()
    }

    fun hasActiveTaskForSource(source: IndexSource): Boolean {
        if (activeTaskCache.any { it.host == source.host }) {
            return true
        }

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
            refreshInterval = source.refreshInterval
        )

        taskStore.save(task)
        meter.counter("$service.enqueued", "host", task.host).increment()
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun monitorTasks() {
        val activeTasks = taskStore.getTasks()

        activeTasks.forEach {
            val tags = arrayOf("status", it.status!!.name, "host", it.host)
            log.info { "Task: $it is in state: ${it.status}" }
            meter.counter("$service.task-count", *tags).increment()

            when(it.status!!) {
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
                        it.monitorTimestamp = it.monitorTimestamp ?: Instant.now().toEpochMilli()
                    }

                    if (!isTaskComplete(it)) {
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

        activeTaskCache.retainAll(activeTasks.filter {
            it.status == TaskStatus.RUNNING
        }.toSet())
    }

    private fun isTaskComplete(task: Task): Boolean {
        val currentTime = Instant.now()

        return task.monitorTimestamp?.let {
            Instant.ofEpochMilli(it).plus(taskMonitorConfiguration.emptyQueueMonitorDuration) < currentTime
        }?: false
    }

    private fun hasIndexTasksInQueue(task: Task): Boolean {
        val taskCount = indexingTaskQueueClient.getTaskCount(task.queueUrl!!)
        return taskCount > 0
    }

    private fun seedIndexTaskQueue(task: Task) {
        val taskRunId = UUID.randomUUID().toString()

        log.info { "Generating seed tasks for: ${task.host} with: ${task.seeds}" }
        log.info { "Task run id: $taskRunId" }

        val seedTasks = task.seeds!!.map {
            IndexTask(
                source = task.queueUrl!!,
                details = IndexTaskDetails(
                    pageUrl = URL(it),
                    refreshIntervalSeconds = task.refreshInterval!!,
                    taskType = IndexTaskType.HTML,
                    submitTime = Instant.now().toEpochMilli(),
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