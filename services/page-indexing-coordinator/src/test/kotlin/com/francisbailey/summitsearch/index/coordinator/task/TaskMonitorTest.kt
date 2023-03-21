package com.francisbailey.summitsearch.index.coordinator.task

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class TaskMonitorTest {
    private val taskStore =  mock<TaskStore>()
    private val emptyQueueMonitorDuration = Duration.ofSeconds(30)
    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()
    private val meterRegistry = SimpleMeterRegistry()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

    @Test
    fun `transitions task from pending to running`() {
        val taskMonitor = TaskMonitor(
            taskStore,
            emptyQueueMonitorDuration,
            indexingTaskQueueClient,
            meterRegistry,
            clock
        )

        val task = Task(
            id = "abc-123",
            host = "http://some-host.com",
            queueUrl = "some-queue-url",
            status = TaskStatus.PENDING,
            seeds = setOf("http://some-host.com/page1")
        )

        whenever(taskStore.getTasks()).thenReturn(listOf(task))

        taskMonitor.monitorTasks()

        verify(indexingTaskQueueClient).addTasks(org.mockito.kotlin.check {
            val queuedTask = it.first()
            assertEquals(queuedTask.details.entityUrl.toString(), task.seeds.first())
            assertEquals(queuedTask.details.taskType, IndexTaskType.HTML)
            assertEquals(queuedTask.details.submitTime, clock.instant().toEpochMilli())
        })
        verify(taskStore).save(org.mockito.kotlin.check {
            assertEquals(TaskStatus.RUNNING, it.status)
        })
    }

    @Test
    fun `sets monitor timestamp on running task if queue is empty`() {
        val taskMonitor = TaskMonitor(
            taskStore,
            emptyQueueMonitorDuration,
            indexingTaskQueueClient,
            meterRegistry,
            clock
        )

        val task = Task(
            id = "abc-123",
            host = "http://some-host.com",
            queueUrl = "some-queue-url",
            status = TaskStatus.RUNNING,
            seeds = setOf("http://some-host.com/page1")
        )

        whenever(taskStore.getTasks()).thenReturn(listOf(task))
        whenever(indexingTaskQueueClient.getTaskCount(any())).thenReturn(0)

        taskMonitor.monitorTasks()

        verify(taskStore).save(org.mockito.kotlin.check {
            assertEquals(TaskStatus.RUNNING, it.status)
            assertEquals(clock.instant().toEpochMilli(), it.monitorTimestamp)
        })
    }

    @Test
    fun `transitions running task to completed if queue is empty for configured time`() {
        val taskMonitor = TaskMonitor(
            taskStore,
            emptyQueueMonitorDuration,
            indexingTaskQueueClient,
            meterRegistry,
            clock
        )

        val task = Task(
            id = "abc-123",
            host = "http://some-host.com",
            queueUrl = "some-queue-url",
            status = TaskStatus.RUNNING,
            seeds = setOf("http://some-host.com/page1"),
            monitorTimestamp = clock
                .instant()
                .minusSeconds(emptyQueueMonitorDuration.seconds)
                .toEpochMilli()
        )

        whenever(taskStore.getTasks()).thenReturn(listOf(task))
        whenever(indexingTaskQueueClient.getTaskCount(any())).thenReturn(0)

        taskMonitor.monitorTasks()

        verify(taskStore).save(org.mockito.kotlin.check {
            assertEquals(TaskStatus.COMPLETED, it.status)
        })
    }

    @Test
    fun `deletes task in COMPLETED state`() {
        val taskMonitor = TaskMonitor(
            taskStore,
            emptyQueueMonitorDuration,
            indexingTaskQueueClient,
            meterRegistry,
            clock
        )

        val task = Task(
            id = "abc-123",
            host = "http://some-host.com",
            queueUrl = "some-queue-url",
            status = TaskStatus.COMPLETED,
            seeds = setOf("http://some-host.com/page1"),
            monitorTimestamp = clock.instant().toEpochMilli()
        )

        whenever(taskStore.getTasks()).thenReturn(listOf(task))

        taskMonitor.monitorTasks()

        verify(taskStore).delete(org.mockito.kotlin.check {
            assertEquals(task.host, it.host)
            assertEquals(task.id, it.id)
        })
    }

    @Test
    fun `does not override monitorTimestamp on subsequent runs`() {
        val timestamp = clock.instant().plusSeconds(20).toEpochMilli()
        val taskMonitor = TaskMonitor(
            taskStore,
            emptyQueueMonitorDuration,
            indexingTaskQueueClient,
            meterRegistry,
            clock
        )

        val task = Task(
            id = "abc-123",
            host = "http://some-host.com",
            queueUrl = "some-queue-url",
            status = TaskStatus.RUNNING,
            seeds = setOf("http://some-host.com/page1")
        )

        whenever(taskStore.getTasks()).thenReturn(listOf(task))
        whenever(indexingTaskQueueClient.getTaskCount(any())).thenReturn(0)

        taskMonitor.monitorTasks()

        verify(taskStore).save(org.mockito.kotlin.check {
            assertEquals(TaskStatus.RUNNING, it.status)
            assertEquals(clock.instant().toEpochMilli(), it.monitorTimestamp)
        })

        task.monitorTimestamp = timestamp // reset this as it shouldn't be set
        taskMonitor.monitorTasks()

        verify(taskStore, times(2)).save(org.mockito.kotlin.check {
            assertEquals(TaskStatus.RUNNING, it.status)
            assertEquals(timestamp, it.monitorTimestamp)
        })
    }
}