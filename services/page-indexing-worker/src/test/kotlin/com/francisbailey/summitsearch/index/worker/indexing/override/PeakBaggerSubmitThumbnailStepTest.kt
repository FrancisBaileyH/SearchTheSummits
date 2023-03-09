package com.francisbailey.summitsearch.index.worker.indexing.override

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.URL
import java.time.Duration
import java.util.*

class PeakBaggerSubmitThumbnailStepTest: StepTest() {

    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()

    private val step = PeakBaggerSubmitThumbnailStep(indexingTaskQueueClient)

    @Test
    fun `submits image if one is present`() {
        val expectedImageSrc = "https://peakbaggerblobs.blob.core.windows.net/pbphoto/p21869.jpg"
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                pageUrl = URL("https://peakbagger.com/climber/ascent.aspx?aid=1761962"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem<DatedDocument>(
            task = task,
            payload = DatedDocument(
                pageCreationDate = null,
                document = loadHtml("peakbagger/TripReportWithImage.html")
            )
        )

        val result = step.process(item, monitor)

        verify(indexingTaskQueueClient).addTask(org.mockito.kotlin.check {
            assertEquals(task.source, it.source)
            assertEquals(URL(expectedImageSrc), it.details.pageUrl)
            assertEquals(IndexTaskType.THUMBNAIL, it.details.taskType)
            assertEquals(task.details.taskRunId, it.details.taskRunId)
            assertEquals(task.details.pageUrl, it.details.getContext<ImageTaskContext>()?.referencingURL)
        })

        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)
    }
}