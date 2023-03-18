package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * @TODO - all unit tests
 * @TODO - heart beat time out on worker itself
 */
@Service
class WorkerAssignmentCoordinator(
    private val taskMonitor: TaskMonitor,
    private val workerClient: WorkerClient,
    private val meter: MeterRegistry,
    private val workerHealthTracker: WorkerHealthTracker
) {
    private val log = KotlinLogging.logger { }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    fun coordinate() {
        val taskQueue = ArrayDeque<Task>()
        val activeTasks = taskMonitor.getActiveTasks()
        val workers = workerHealthTracker.getHealthyWorkers()

        taskQueue.addAll(activeTasks)

        try {
            workers.forEach {
                workerClient.clearAssignments(it)
                log.info { "Cleared assignment on: $it" }
            }

            workers.forEach {
                val assignments = (1.. it.availableSlots).mapNotNull {
                    taskQueue.removeFirstOrNull()
                }

                if (assignments.isNotEmpty()) {
                    workerClient.addAssignments(it, assignments)
                    log.info { "Successfully assigned: $assignments to $it" }
                    meter.counter("$serviceName.assignment", "status", "success", "worker", it.endpoint)
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to coordinate with workers" }
            meter.counter("$serviceName.assignment", "status", "failed")
        }
    }

    companion object {
        const val serviceName = "worker-coordinator"
    }
}