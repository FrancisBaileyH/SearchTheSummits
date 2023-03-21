package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.store.PageMetadataStore
import com.francisbailey.summitsearch.index.worker.store.PageMetadataStoreItem
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
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

    private val documentFilterService = mock<DocumentFilterService> {
        on(mock.shouldFilter(any())).thenReturn(false)
    }

    private val pageMetadataStoreItem = mock<PageMetadataStoreItem>()

    private val meterRegistry = SimpleMeterRegistry()

    private val defaultURL = URL("https://francisbailey.com")


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
        verifyNoInteractions(documentFilterService)
    }

    @Test
    fun `ignores external links`() {
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)

        buildTask("https://some-other-page.com").run()

        verify(pageMetadataStore).saveDiscoveryMetadata("some-other-page.com")
        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(documentFilterService)
    }

    @Test
    fun `ignores link if it is the same as the associated task`() {
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)

        buildTask("https://francisbailey.com").run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(pageMetadataStore)
        verifyNoInteractions(documentFilterService)
    }

    @Test
    fun `ignores link if filter service returns true on should filter`() {
        val discovery = URL("https://francisbailey.com/wp-content/some-file.jpg")
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(documentFilterService.shouldFilter(any())).thenReturn(true)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verifyNoInteractions(pageMetadataStore)
        verify(documentFilterService).shouldFilter(discovery)
    }

    @Test
    fun `skips link if metadata is too new`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(discovery)
    }

    @Test
    fun `normalizes links before consulting the store`() {
        val discovery = URL("https://francisbailey.com/test/test2?query=x#someFragment")
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery.toString()).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(URL("https://francisbailey.com/test/test2?query=x"))
    }

    @Test
    fun `encodes white spaces before creating URL`() {
        val discovery ="https://francisbailey.com/test/test with spaces here.pdf"
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(URL("https://francisbailey.com/test/test%20with%20spaces%20here.pdf"))
    }

    @Test
    fun `handles already encoded URLs`() {
        val discovery = "https://francisbailey.com/test/test%20with%20spaces%20here.pdf"
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(false)

        buildTask(discovery).run()

        verifyNoInteractions(taskQueueClient)
        verify(pageMetadataStore).getMetadata(URL("https://francisbailey.com/test/test%20with%20spaces%20here.pdf"))
    }

    @Test
    fun `if all conditions met then link is added to task queue and saved to task store`() {
        val discovery = URL("https://francisbailey.com/test")
        whenever(indexTaskDetails.refreshDuration()).thenReturn(Duration.ofMinutes(10))
        whenever(indexTaskDetails.entityUrl).thenReturn(defaultURL)
        whenever(pageMetadataStore.getMetadata(any())).thenReturn(pageMetadataStoreItem)
        whenever(indexTaskDetails.taskType).thenReturn(IndexTaskType.HTML)
        whenever(pageMetadataStoreItem.canRefresh(any())).thenReturn(true)

        buildTask(discovery.toString(), type = IndexTaskType.PDF).run()

        verify(pageMetadataStore).getMetadata(discovery)
        verify(pageMetadataStoreItem).canRefresh(indexTaskDetails.refreshDuration())
        verify(taskQueueClient).addTask(check {
            assertEquals(it.details.entityUrl, discovery)
            assertEquals(it.source, associatedTask.source)
            assertEquals(it.details.taskRunId, indexTaskDetails.taskRunId)
            assertEquals(it.details.taskType, IndexTaskType.PDF)
        })
        verify(pageMetadataStore).saveMetadata(indexTaskDetails.taskRunId, discovery)
    }


    private fun buildTask(discovery: String, type: IndexTaskType = IndexTaskType.HTML) = LinkDiscoveryTask(
        taskQueueClient,
        pageMetadataStore,
        associatedTask,
        documentFilterService,
        meterRegistry,
        Discovery(type, discovery)
    )
}