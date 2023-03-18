package com.francisbailey.summitsearch.index.coordinator.task

import com.francisbailey.summitsearch.index.coordinator.DynamoDBLocal
import com.francisbailey.summitsearch.index.coordinator.configuration.TaskStoreConfiguration
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class TaskStoreTest {

    private val regionConfig = mock<RegionConfig> {
        on(mock.isProd).thenReturn(false)
    }

    private val configuration = TaskStoreConfiguration(
        regionConfig,
        dynamoDBLocal.asyncWaiter(),
        dynamoDBLocal.enhancedAsyncClient()
    )

    private val taskStore = TaskStore(
        configuration.taskStoreTable(),
        SimpleMeterRegistry()
    )

    @Test
    fun `saved item is retrievable from store`() {
        val host = "example.com"
        val task = Task(
            id = "abc123",
            host = host,
            status = TaskStatus.RUNNING,
            queueUrl = "",
            monitorTimestamp = null,
            seeds = setOf("example.com"),
            refreshInterval = 0
        )

        taskStore.save(task)

        val result = taskStore.getTask(host)

        assertEquals(task, result)
    }

    @Test
    fun `multiple items saved are retrievable with getAllTasks() call`() {
        val tasks = (0..10).map {
            Task(
                id = "$it",
                host = "http://example$it.com",
                status = TaskStatus.RUNNING,
                queueUrl = "",
                monitorTimestamp = null,
                seeds = setOf("example.com"),
                refreshInterval = 0
            )
        }.toSet()

        tasks.forEach {
            taskStore.save(it)
        }

        val savedTasks = taskStore.getTasks()

        assertEquals(tasks.size, savedTasks.size)
        assertTrue(tasks.containsAll(savedTasks))
    }

    @Test
    fun `deletes task successfully`() {
        val host = "test.com"
        val task = Task(
            id = "abc1231",
            host = host,
            status = TaskStatus.RUNNING,
            queueUrl = "",
            monitorTimestamp = null,
            seeds = setOf("example.com"),
            refreshInterval = 0
        )

        taskStore.save(task)

        val result = taskStore.getTask(host)

        assertEquals(task, result)

        taskStore.delete(task)
        assertNull(taskStore.getTask(host))
    }

    companion object {
        private val dynamoDBLocal = DynamoDBLocal.global()
    }
}