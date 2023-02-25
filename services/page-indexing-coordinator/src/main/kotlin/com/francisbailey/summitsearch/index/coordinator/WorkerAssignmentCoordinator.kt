package com.francisbailey.summitsearch.index.coordinator

import com.francisbailey.summitsearch.index.worker.api.GetAssignmentsResponse
import com.francisbailey.summitsearch.taskclient.IndexTask
import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class WorkerAssignmentCoordinator(
    private val taskMonitor: TaskMonitor,
    private val workerClient: WorkerClient,
    private val healthyWorkerTracker: HealthyWorkerTracker,
    private val workerConfiguration: WorkerConfiguration,
) {

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun coordinate() {
        val taskQueue = ArrayDeque<Task>()
        val activeTasks = taskMonitor.getActiveTasks()
        val workers = healthyWorkerTracker.getHealthyWorkers()

        taskQueue.addAll(activeTasks)

        // Easiest thing to do is just clear all assignments
        workers.forEach {
            workerClient.clearAssignments(it)
        }

        workers.forEach {
            val assignments = (1.. workerConfiguration.maxAssignmentsPerWorker).mapNotNull {
                taskQueue.removeFirstOrNull()
            }

            if (assignments.isNotEmpty()) {
                workerClient.addAssignments(it, assignments)
            }
        }
    }
}


/**
 * WebSourcesMonitor?
 */

@Service
class WebSourceRefreshMonitor(
    private val indexingTaskQueueClient: IndexingTaskQueueClient,
    private val indexSourceRepository: IndexSourceRepository,
    private val taskMonitor: TaskMonitor
) {

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    fun checkSources() {
        val sources = indexSourceRepository.getRefreshableSources()

        sources.forEach {
            val queueName = "IndexingQueue-${it.host.replace(".", "-")}"

            if (indexingTaskQueueClient.queueExists(queueName)) {
                indexingTaskQueueClient.createQueue(queueName)
            }

            if (!taskMonitor.hasActiveTaskForSource(it)) {

                IndexTask(
                    source =
                )

                indexingTaskQueueClient.addTask(IndexT)

                taskMonitor.enqueueTaskForSource(it)
            }
        }
    }

}

@Configuration
open class TaskMonitorConfiguration {
    val emptyQueueMonitorDuration = Duration.ofMinutes(30)
}

/**
 * TaskMonitor needs to....
 *
 * 1. Check for overdueSources
 * 2. Create a queue if it does not exist
 * 3. Create a task if it does not exist
 * 4. Close a task if it's completed
 */
@Service
class TaskMonitor(
    private val taskStore: TaskStore,
    private val taskMonitorConfiguration: TaskMonitorConfiguration,
    private val indexingTaskQueueClient: IndexingTaskQueueClient
) {
    private val activeTaskCache = mutableSetOf<Task>()

    fun getActiveTasks(): List<Task> = synchronized(this) {
        return activeTaskCache.toList()
    }

    fun isTaskActive(task: Task): Boolean {
        val currentTime = Instant.now()

        return task.monitorTimestamp?.let {
            Instant.ofEpochMilli(it).plus(taskMonitorConfiguration.emptyQueueMonitorDuration) < currentTime
        }?: false
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
            monitorTimestamp = null
        )

        taskStore.save(task)
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun monitorTasks() {
        val activeTasks = taskStore.getTasks()

        activeTasks.forEach {
            when(it.status) {
                TaskStatus.PENDING -> {
                    indexingTaskQueueClient.addTask(Ind)

                    // plant seed/add task here
                    taskStore.save(it.apply {
                        status = TaskStatus.RUNNING
                    })
                }
                TaskStatus.RUNNING -> {
                    // if (queueIsEmpty()) {
                    //   monitorTimestamp = Instant.now().toEpochMillis()
                    // else
                    //  monitorTimestamp = null
                    if (!isTaskActive(it)) {
                        it.status = TaskStatus.COMPLETED
                    }

                    taskStore.save(it)
                }
                else -> {
                    taskStore.delete(it)
                }
            }
        }

        activeTaskCache.retainAll(activeTasks.filter {
            it.status == TaskStatus.RUNNING
        }.toSet())
    }
}


data class IndexSource(
    val host: String,
    val seeds: Set<String>,
    val lastUpdate: Long?,
    val refreshInterval: Long,
    val documentTtl: Long,
    val queueUrl: String,
)

@Service
class IndexSourceRepository {

    fun getRefreshableSources(): List<IndexSource> {
        return emptyList()
    }
}

data class Worker(
    val workerEndpoint: WorkerEndpoint,
    var availableSlots: Int
)

@Configuration
open class WorkerConfiguration {
    val maxAssignmentsPerWorker = 3
}

@Service
class WorkerClient {

    fun getAssignments(workerEndpoint: Worker): GetAssignmentsResponse {
        return GetAssignmentsResponse(assignments = emptySet())
    }

    fun clearAssignments(workerEndpoint: Worker) {

    }

    fun addAssignments(workerEndpoint: Worker, assignments: List<Task>) {

    }
}


@Service
class HealthyWorkerTracker {
    fun getHealthyWorkers(): List<Worker> {
        return emptyList()
    }

}


@Service
class TaskStore {

    fun getTask(host: String): Task? {
        return null
    }

    fun getTasks(): List<Task> {
        return emptyList()
    }

    fun delete(task: Task) {

    }

    fun save(task: Task) {

    }

    fun createTask(source: IndexSource): Task {
        return Task("", "", TaskStatus.PENDING,  "", null)
    }
}

data class WorkerEndpoint(
    val endpoint: String
)

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED
}

data class Task(
    val id: String,
    val host: String,
    var status: TaskStatus,
    var queueUrl: String,
    var monitorTimestamp: Long?
)

