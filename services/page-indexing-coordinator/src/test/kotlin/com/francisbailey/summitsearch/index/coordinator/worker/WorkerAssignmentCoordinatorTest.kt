package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class WorkerAssignmentCoordinatorTest {

    private val taskMonitor = mock<TaskMonitor>()
    private val workerClient = mock<WorkerClient>()
    private val meter = SimpleMeterRegistry()
    private val workerHealthTracker = mock<WorkerHealthTracker>()

    private val coordinator = WorkerAssignmentCoordinator(
        taskMonitor,
        workerClient,
        meter,
        workerHealthTracker
    )

    private val workerA = mock<Worker> {
        on(mock.endpoint).thenReturn("workerA")
    }
    private val workerB = mock<Worker> {
        on(mock.endpoint).thenReturn("workerB")
    }

    private val task = mock<Task>()

    @Test
    fun `deletes all assignments before reassignment`() {
        whenever(workerHealthTracker.getHealthyWorkers()).thenReturn(setOf(workerA))
        whenever(taskMonitor.getActiveTasks()).thenReturn(emptyList())

        coordinator.coordinate()

        verify(workerClient).clearAssignments(workerA)
        verifyNoMoreInteractions(workerClient)
    }

    @Test
    fun `assigns up to maximum allowed assignments per host`() {
        val maxSlots = 3

        whenever(workerA.availableSlots).thenReturn(maxSlots)

        val tasks = (0..maxSlots + 1).map {
            mock<Task>()
        }

        whenever(workerHealthTracker.getHealthyWorkers()).thenReturn(setOf(workerA))
        whenever(taskMonitor.getActiveTasks()).thenReturn(tasks)

        coordinator.coordinate()

        verify(workerClient).clearAssignments(workerA)
        verify(workerClient).addAssignments(eq(workerA), check {
            assertEquals(maxSlots, it.size)
        })
    }
}