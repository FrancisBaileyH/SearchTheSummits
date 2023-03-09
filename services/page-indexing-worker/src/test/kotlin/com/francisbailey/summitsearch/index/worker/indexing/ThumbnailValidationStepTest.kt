package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.ThumbnailValidationStep
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.sksamuel.scrimage.ImmutableImage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URL
import java.time.Duration
import java.util.*

class ThumbnailValidationStepTest: StepTest() {

    private val indexService = mock<SummitSearchIndexService>()

    private val step = ThumbnailValidationStep(indexService)

    private val task = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.THUMBNAIL,
            refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
            context = Json.encodeToString(ImageTaskContext(
                referencingURL = URL("https://francisbaileyh.com/test"),
                description = ""
            ))
        )
    )

    private val item = PipelineItem<ImmutableImage>(task, null)

    @Test
    fun `does not continue processing on exception`() {
        whenever(indexService.pageExists(any())).thenThrow(RuntimeException("Test Failure"))
        assertThrows<RuntimeException> { step.process(item, monitor) }
    }

    @Test
    fun `does not continue processing if page does not exist`() {
        whenever(indexService.pageExists(any())).thenReturn(false)

        val result = step.process(item, monitor)

        assertFalse(result.continueProcessing)
        verify(indexService).pageExists(org.mockito.kotlin.check {
            assertEquals(task.details.getContext<ImageTaskContext>()?.referencingURL, it.source)
        })
    }

    @Test
    fun `does continue processing if page exists`() {
        whenever(indexService.pageExists(any())).thenReturn(true)

        val result = step.process(item, monitor)

        assertTrue(result.continueProcessing)
        verify(indexService).pageExists(org.mockito.kotlin.check {
            assertEquals(task.details.getContext<ImageTaskContext>()?.referencingURL, it.source)
        })
    }

}