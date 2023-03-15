package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.configuration.WorkerConfiguration
import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class WorkerAssignmentCoordinator(
    private val taskMonitor: TaskMonitor,
    private val workerClient: WorkerClient,
    private val workerHealthTracker: WorkerHealthTracker,
    private val workerConfiguration: WorkerConfiguration,
) {

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun coordinate() {
        val taskQueue = ArrayDeque<Task>()
        val activeTasks = taskMonitor.getActiveTasks()
        val workers = workerHealthTracker.getHealthyWorkers()

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