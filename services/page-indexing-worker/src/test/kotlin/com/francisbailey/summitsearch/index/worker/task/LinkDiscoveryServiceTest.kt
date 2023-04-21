package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.store.PageMetadataStore
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.time.Instant
import java.util.concurrent.Executor

class LinkDiscoveryServiceTest {

    private val linkDiscoveryTaskExecutor = mock<Executor>()
    private val taskQueueClient = mock<IndexingTaskQueueClient>()
    private val pageMetadataStore = mock<PageMetadataStore>()
    private val documentFilterService = mock<DocumentFilterService>()
    private val meterRegistry = SimpleMeterRegistry()

    private val linkDiscoveryService = LinkDiscoveryService(linkDiscoveryTaskExecutor, taskQueueClient, pageMetadataStore, documentFilterService, meterRegistry)

    private val associatedTask = mock<IndexTask>()

    @Test
    fun `omits empty link submissions`() {
        val discoveries = listOf(
            "",
            ""
        ).map {
            Discovery(IndexTaskType.HTML, it)
        }

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
        ).map {
            Discovery(IndexTaskType.HTML, it)
        }

        linkDiscoveryService.submitDiscoveries(associatedTask, discoveries)

        argumentCaptor<Runnable>().apply {
            verify(linkDiscoveryTaskExecutor, times(3)).execute(capture())
            assertEquals("https://francisbailey.com/test", (firstValue as LinkDiscoveryTask).discovery.source)
            assertEquals("https://francisbailey.com/test2", (secondValue as LinkDiscoveryTask).discovery.source)
            assertEquals("https://someexternalsite.com", (thirdValue as LinkDiscoveryTask).discovery.source)
        }

        verifyNoMoreInteractions(linkDiscoveryTaskExecutor)
    }

    @Test
    fun `submits one image discovery task per call`() {
        val discoveries = (0..9).map {
            ImageDiscovery(
                source = "abc$it",
                referencingURL = URL("https://www.exmaple.com"),
                pageCreationDate = Instant.now().toEpochMilli(),
                description = "ABC $it",
                type = ImageDiscoveryType.THUMBNAIL
            )
        }.toSet()

        linkDiscoveryService.submitImages(associatedTask, discoveries)

        argumentCaptor<Runnable>().apply {
            verify(linkDiscoveryTaskExecutor, times(1)).execute(capture())
            assertTrue(firstValue is ImageDiscoveryTask)
        }

        verifyNoMoreInteractions(linkDiscoveryTaskExecutor)
    }

}