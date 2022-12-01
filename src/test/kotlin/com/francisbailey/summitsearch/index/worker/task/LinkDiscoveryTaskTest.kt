package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.metadata.PageMetadataStore
import com.francisbailey.summitsearch.index.worker.metadata.PageMetadataStoreItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration

class LinkDiscoveryTaskTest {

    private val taskQueueClient =  mock<IndexingTaskQueueClient>()
    private val pageMetadataStore = mock<PageMetadataStore>()
    private val indexTaskDetails = mock<IndexTaskDetails> {
        on(mock.id).thenReturn("test123")
        on(mock.taskRunId).thenReturn("taskRunId123")
    }
    private val associatedTask = mock<IndexTask> {
        on(mock.details).thenReturn(indexTaskDetails)
        on(mock.source).thenReturn("some-queue")
    }

    private val pageMetadataStoreItem = mock<PageMetadataStoreItem>()


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
        verifyNoInteractions(pageMetadataStore)
    }

    @Test
    fun `ignores external links`() {
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")

        buildTask("https://some-other-page.com").run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(pageMetadataStore)
    }

    @Test
    fun `ignores link if it is the same as the associated task`() {
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")

        buildTask("https://francisbailey.com").run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(pageMetadataStore)
    }

    @Test
    fun `skips link if metadata is too new`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(discovery)
    }

    @Test
    fun `normalizes links before consulting the store`() {
        val discovery = URL("https://francisbailey.com/test?query=x#someFragment")
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(URL("https://francisbailey.com/test?query=x"))
    }

    @Test
    fun `if all conditions met then link is added to task queue and saved to task store`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(indexTaskDetails.pageUrl).thenReturn("https://francisbailey.com")
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(true)

        buildTask(discovery.toString()).run()

        verify(pageMetadataStore).getMetadata(discovery)
        verify(pageMetadataStoreItem).canRefresh(indexTaskDetails.refreshDuration())
        verify(taskQueueClient).addTask(check {
            assertEquals(it.details.pageUrl, discovery.toString())
            assertEquals(it.source, associatedTask.source)
            assertEquals(it.details.taskRunId, indexTaskDetails.taskRunId)
        })
        verify(pageMetadataStore).saveMetadata(indexTaskDetails.taskRunId, discovery)
    }


    private fun buildTask(discovery: String) = LinkDiscoveryTask(
        taskQueueClient,
        pageMetadataStore,
        associatedTask,
        discovery
    )
}