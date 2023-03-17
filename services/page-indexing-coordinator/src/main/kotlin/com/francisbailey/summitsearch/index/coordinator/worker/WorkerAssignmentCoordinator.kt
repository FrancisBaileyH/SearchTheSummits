package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.configuration.WorkerConfiguration
import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * @TODO - heart beat client for workers
 * @TODO - all unit tests
 * @TODO - heart beat time out on worker itself
 * @TODO - create table if not exists logic
 */
@Service
class WorkerAssignmentCoordinator(
    private val taskMonitor: TaskMonitor,
    private val workerClient: WorkerClient,
    private val meter: MeterRegistry,
    private val workerHealthTracker: WorkerHealthTracker,
    private val workerConfiguration: WorkerConfiguration,
) {
    private val log = KotlinLogging.logger { }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun coordinate() {
        val taskQueue = ArrayDeque<Task>()
        val activeTasks = taskMonitor.getActiveTasks()
        val workers = workerHealthTracker.getHealthyWorkers()

        taskQueue.addAll(activeTasks)

        // Easiest thing to do is just clear all assignments
        workers.forEach {
            // what if this step fails?d
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