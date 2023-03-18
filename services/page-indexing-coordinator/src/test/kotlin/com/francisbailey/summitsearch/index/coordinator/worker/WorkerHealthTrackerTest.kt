package com.francisbailey.summitsearch.index.coordinator.worker

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WorkerHealthTrackerTest {

    private val workerClient = mock<WorkerClient>()

    private val healthyWorker = Worker(
        "worker1",
        availableSlots = 3
    )

    private val experimentWorker = Worker(
        "worker2",
        availableSlots = 3
    )

    private val workers = setOf(healthyWorker, experimentWorker)

    @Test
    fun `hosts are marked as healthy when recovery threshold is reached`() {
        val tracker = WorkerHealthTracker(
            workerClient = workerClient,
            workers = workers
        )

        assertTrue(tracker.getHealthyWorkers().isEmpty())

        repeat(WorkerHealthTracker.RECOVERY_THRESHOLD) {
            tracker.monitorWorkers()
        }

        assertTrue(tracker.getHealthyWorkers().isEmpty())

        repeat(1) {
            tracker.monitorWorkers()
        }

        assertEquals(workers, tracker.getHealthyWorkers())
    }

    @Test
    fun `host is marked as unhealthy after MAX_FAIL_COUNT is reached`() {
        val tracker = WorkerHealthTracker(
            workerClient = workerClient,
            workers = workers
        )

        repeat(WorkerHealthTracker.RECOVERY_THRESHOLD + 1) {
            tracker.monitorWorkers()
        }
        assertEquals(workers, tracker.getHealthyWorkers())

        whenever(workerClient.sendHeartBeat(experimentWorker)).thenThrow(RuntimeException("Test"))

        repeat(WorkerHealthTracker.MAX_FAIL_COUNT - 1) {
            tracker.monitorWorkers()
        }
        assertEquals(workers, tracker.getHealthyWorkers())

        tracker.monitorWorkers()

        assertEquals(1, tracker.getHealthyWorkers().size)
        assertEquals(healthyWorker, tracker.getHealthyWorkers().first())
    }

    @Test
    fun `host recovers after being removed from pool`() {
        val tracker = WorkerHealthTracker(
            workerClient = workerClient,
            workers = workers
        )
        // whenever doesn't like overriding void method calls
        var returnFailure = false

        whenever(workerClient.sendHeartBeat(experimentWorker)).then {
            if (returnFailure) {
                throw RuntimeException("Test")
            }
        }

        repeat(WorkerHealthTracker.RECOVERY_THRESHOLD + 1) {
            tracker.monitorWorkers()
        }

        assertEquals(workers, tracker.getHealthyWorkers())

        returnFailure = true

        repeat(WorkerHealthTracker.MAX_FAIL_COUNT) {
            tracker.monitorWorkers()
        }

        assertEquals(1, tracker.getHealthyWorkers().size)
        assertEquals(healthyWorker, tracker.getHealthyWorkers().first())

        returnFailure = false

        repeat(WorkerHealthTracker.RECOVERY_THRESHOLD + 1) {
            tracker.monitorWorkers()
        }

        assertEquals(workers, tracker.getHealthyWorkers())
    }
}