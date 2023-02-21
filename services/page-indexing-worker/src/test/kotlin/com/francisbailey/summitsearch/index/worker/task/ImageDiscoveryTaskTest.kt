package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*

class ImageDiscoveryTaskTest {

    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()

    private val associatedTask = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.HTML,
            refreshIntervalSeconds = Duration.ofMinutes(60).seconds
        )
    )

    @Test
    fun `submits images as single batch when less than 10`() {
        val discoveries = (0..9).map {
            ImageDiscovery(
                source = "http://abc$it.com",
                referencingURL = URL("https://www.exmaple.com"),
                pageCreationDate = Instant.now().toEpochMilli(),
                description = "ABC $it"
            )
        }


        val task = ImageDiscoveryTask(indexingTaskQueueClient, discoveries.toSet(), associatedTask)

        task.run()

        verify(indexingTaskQueueClient).addTasks(org.mockito.kotlin.check {
            it.forEachIndexed { index, task ->
                assertEquals(task.source, associatedTask.source)
                assertEquals(task.details.pageUrl.toString(), discoveries[index].source)
                assertEquals(task.details.taskType, IndexTaskType.IMAGE)
                assertEquals(
                    task.details.context, Json.encodeToString(
                        ImageTaskContext(
                            referencingURL = discoveries[index].referencingURL,
                            description = discoveries[index].description,
                            pageCreationDate = discoveries[index].pageCreationDate
                        )
                    )
                )
            }
        })
    }

    @Test
    fun `skips discoveries with a bad url`() {
        val discoveries = (0..9).map {
            ImageDiscovery(
                source = "bad-url",
                referencingURL = URL("https://www.exmaple.com"),
                pageCreationDate = Instant.now().toEpochMilli(),
                description = "ABC $it"
            )
        }.toMutableList()

        val expectedDiscovery = ImageDiscovery(
            source = "http://good-url.com",
            referencingURL = URL("https://www.exmaple.com"),
            pageCreationDate = Instant.now().toEpochMilli(),
            description = "ABC 1"
        )

        discoveries.add(expectedDiscovery)

        val task = ImageDiscoveryTask(indexingTaskQueueClient, discoveries.toSet(), associatedTask)

        task.run()

        verify(indexingTaskQueueClient).addTasks(org.mockito.kotlin.check {
            val capturedTask = it.first()
            assertEquals(1, it.size)
            assertEquals(capturedTask.source, associatedTask.source)
            assertEquals(capturedTask.details.pageUrl.toString(), expectedDiscovery.source)
            assertEquals(capturedTask.details.taskType, IndexTaskType.IMAGE)
            assertEquals(capturedTask.details.context, Json.encodeToString(ImageTaskContext(
                referencingURL = expectedDiscovery.referencingURL,
                description = expectedDiscovery.description,
                pageCreationDate = expectedDiscovery.pageCreationDate
            )))
        })
    }

    @Test
    fun `submits images as multiple batches when more than 10 discoveries`() {
        val discoveries = (0..11).map {
            ImageDiscovery(
                source = "http://abc$it.com",
                referencingURL = URL("https://www.exmaple.com"),
                pageCreationDate = Instant.now().toEpochMilli(),
                description = "ABC $it"
            )
        }


        val task = ImageDiscoveryTask(indexingTaskQueueClient, discoveries.toSet(), associatedTask)

        task.run()

        verify(indexingTaskQueueClient, times(2)).addTasks(any())
    }
}