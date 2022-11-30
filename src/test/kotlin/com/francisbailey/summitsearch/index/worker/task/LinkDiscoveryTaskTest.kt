package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.client.TaskStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL

class LinkDiscoveryTaskTest {

    private val taskQueueClient =  mock<IndexingTaskQueueClient>()
    private val taskStore = mock<TaskStore>()
    private val indexTaskDetails = mock<IndexTaskDetails> {
        on(mock.id).thenReturn("test123")
        on(mock.taskRunId).thenReturn("taskRunId123")
    }
    private val associatedTask = mock<IndexTask> {
        on(mock.details).thenReturn(indexTaskDetails)
        on(mock.source).thenReturn("some-queue")
    }


    @Test
    fun `ignores links beyond max links size`() {
        val discovery = StringBuffer().apply {
            this.append("https://francisbailey.com/path/")
            repeat(LinkDiscoveryTask.MAX_LINK_SIZE - this.length + 1) {
                this.append("a")
            }
        }
        val task = buildTask(discovery.toString())

        task.run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(taskStore)
    }

    @Test
    fun `ignores external links`() {
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")

        buildTask("https://some-other-page.com").run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(taskStore)
    }

    @Test
    fun `ignores link if it is the same as the associated task`() {
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")

        buildTask("https://francisbailey.com").run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(taskStore)
    }

    @Test
    fun `skips link if its already mapped in the task store`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(taskStore.hasTask(any(), any())).thenReturn(true)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(taskStore).hasTask(indexTaskDetails.taskRunId, discovery)
    }

    @Test
    fun `normalizes links before consulting the store`() {
        val discovery = URL("https://francisbailey.com/test?query=x#someFragment")
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(taskStore.hasTask(any(), any())).thenReturn(true)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(taskStore).hasTask(indexTaskDetails.taskRunId, URL("https://francisbailey.com/test?query=x"))
    }

    @Test
    fun `if all conditions met then link is added to task queue and saved to task store`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(taskStore.hasTask(indexTaskDetails.taskRunId, discovery)).thenReturn(false)

        buildTask(discovery.toString()).run()

        verify(taskStore).hasTask(indexTaskDetails.taskRunId, discovery)
        verify(taskQueueClient).addTask(check {
            assertEquals(it.details.pageUrl, discovery.toString())
            assertEquals(it.source, associatedTask.source)
            assertEquals(it.details.taskRunId, indexTaskDetails.taskRunId)
        })
        verify(taskStore).saveTask(indexTaskDetails.taskRunId, discovery)
    }


    private fun buildTask(discovery: String) = LinkDiscoveryTask(
        taskQueueClient,
        taskStore,
        associatedTask,
        discovery
    )
}