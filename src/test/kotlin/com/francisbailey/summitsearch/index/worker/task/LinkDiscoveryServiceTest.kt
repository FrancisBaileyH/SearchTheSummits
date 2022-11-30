package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.client.TaskStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.concurrent.Executor

class LinkDiscoveryServiceTest {

    private val linkDiscoveryTaskExecutor = mock<Executor>()
    private val taskQueueClient = mock<IndexingTaskQueueClient>()
    private val taskStore = mock<TaskStore>()

    private val linkDiscoveryService = LinkDiscoveryService(linkDiscoveryTaskExecutor, taskQueueClient, taskStore)

    private val associatedTask = mock<IndexTask>()

    @Test
    fun `omits empty link submissions`() {
        val discoveries = listOf("", "")

        linkDiscoveryService.submitDiscoveries(associatedTask, discoveries)

        verifyNoInteractions(linkDiscoveryTaskExecutor)
    }

    @Test
    fun `submits link discovery task for each unique link`() {
        val discoveries = listOf(
            "https://francisbailey.com/test",
            "https://francisbailey.com/test2", // duplicate
            "https://someexternalsite.com",
            "https://francisbailey.com/test2"
        )

        linkDiscoveryService.submitDiscoveries(associatedTask, discoveries)

        argumentCaptor<Runnable>().apply {
            verify(linkDiscoveryTaskExecutor, times(3)).execute(capture())
            assertEquals("https://francisbailey.com/test", (firstValue as LinkDiscoveryTask).discovery)
            assertEquals("https://francisbailey.com/test2", (secondValue as LinkDiscoveryTask).discovery)
            assertEquals("https://someexternalsite.com", (thirdValue as LinkDiscoveryTask).discovery)
        }

        verifyNoMoreInteractions(linkDiscoveryTaskExecutor)
    }

}