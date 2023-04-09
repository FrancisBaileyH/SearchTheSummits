package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.index.coordinator.task.TaskMonitor
import com.francisbailey.summitsearch.services.common.RegionConfig
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class IndexSourceRefreshMonitorTest {

    private val regionConfig = mock<RegionConfig>()
    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()
    private val indexSourceRepository = mock<IndexSourceStore>()
    private val taskMonitor = mock<TaskMonitor>()
    private val clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

    private val refreshMonitor = IndexSourceRefreshMonitor(
        SimpleMeterRegistry(),
        regionConfig,
        indexingTaskQueueClient,
        indexSourceRepository,
        taskMonitor,
        clock
    )

    @Test
    fun `creates queue if queue url is blank on source`() {
        val source = IndexSource(
            host = "francisbaileyh.com",
            seeds = setOf("http://francisbaileyh.com"),
            nextUpdate = 0,
            documentTtl = 3600,
            refreshIntervalSeconds = 36000,
            queueUrl = ""
        )
        val queueUrl = "some-queue-url"

        whenever(indexingTaskQueueClient.queueExists(any())).thenReturn(true)
        whenever(indexSourceRepository.getRefreshableSources()).thenReturn(listOf(source))
        whenever(indexingTaskQueueClient.createQueue(any())).thenReturn(queueUrl)

        refreshMonitor.checkSources()

        verify(indexingTaskQueueClient).createQueue("sts-index-queue-test-francisbaileyh-com")
        verify(indexSourceRepository, times(2)).save(check {
            assertEquals(queueUrl, it.queueUrl)
        })
    }

    @Test
    fun `creates queue if it does not exist and saves it to store`() {
        val source = IndexSource(
            host = "francisbaileyh.com",
            seeds = setOf("http://francisbaileyh.com"),
            nextUpdate = 0,
            documentTtl = 3600,
            refreshIntervalSeconds = 36000,
            queueUrl = "test"
        )
        val queueUrl = "some-queue-url"

        whenever(indexingTaskQueueClient.queueExists(any())).thenReturn(false)
        whenever(indexSourceRepository.getRefreshableSources()).thenReturn(listOf(source))
        whenever(indexingTaskQueueClient.createQueue(any())).thenReturn(queueUrl)

        refreshMonitor.checkSources()

        verify(indexingTaskQueueClient).createQueue("sts-index-queue-test-francisbaileyh-com")
        verify(indexSourceRepository, times(2)).save(check {
            assertEquals(queueUrl, it.queueUrl)
        })
    }

    @Test
    fun `skips queue creation if it exists`() {
        val source = IndexSource(
            host = "francisbaileyh.com",
            seeds = setOf("http://francisbaileyh.com"),
            nextUpdate = 0,
            documentTtl = 3600,
            refreshIntervalSeconds = 36000,
            queueUrl = "test"
        )

        whenever(indexingTaskQueueClient.queueExists(any())).thenReturn(true)
        whenever(indexSourceRepository.getRefreshableSources()).thenReturn(listOf(source))

        refreshMonitor.checkSources()

        verify(indexingTaskQueueClient, never()).createQueue(any())
        verify(indexSourceRepository).save(source)
    }

    @Test
    fun `enqueues task if there isn't one present already and bumps nextUpdateTime`() {
        Instant.now()
        val source = IndexSource(
            host = "francisbaileyh.com",
            seeds = setOf("http://francisbaileyh.com"),
            nextUpdate = 0,
            documentTtl = 3600,
            refreshIntervalSeconds = 36000,
            queueUrl = "test"
        )

        whenever(indexingTaskQueueClient.queueExists(any())).thenReturn(true)
        whenever(indexSourceRepository.getRefreshableSources()).thenReturn(listOf(source))
        whenever(taskMonitor.hasTaskForSource(any())).thenReturn(false)

        refreshMonitor.checkSources()

        verify(indexingTaskQueueClient, never()).createQueue(any())
        verify(taskMonitor).enqueueTaskForSource(source)
        verify(indexSourceRepository).save(check {
            assertEquals(clock.instant().plusSeconds(source.refreshIntervalSeconds).toEpochMilli(), it.nextUpdate)
        })
    }
}