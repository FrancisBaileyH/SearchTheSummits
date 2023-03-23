package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * This service has a long ways to go before it's ready for a real prod scenario, but
 * it's reached a point where it's good enough to iterate on and get the ball rolling.
 *
 * To reach the level I'd like it to, I'd want the following in place:
 * 1. Service discovery for workers (instead of a hard-coded env list)
 * 2. Distributed locking competition between two or more coordinators (not necessary for now, nor affordable)
 * 3. Asynchronous workload distribution and monitoring. These are all synchronous calls right now, but it's good enough
 * 4. Assignment reconciliation. Rather than refresh all tasks, keep track of current assignments and schedule accordingly
 * 5. More metrics around task management
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

        log.info { "Attempting to assign: ${activeTasks.size} tasks against: ${workers.size} workers" }

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