package com.francisbailey.summitsearch.index.worker.indexing.override

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.URL
import java.time.Duration
import java.util.*

class PeakBaggerSubmitThumbnailStepTest: StepTest() {

    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = PeakBaggerSubmitThumbnailStep(linkDiscoveryService)

    @Test
    fun `submits image if one is present`() {
        val expectedImageSrc = "https://peakbaggerblobs.blob.core.windows.net/pbphoto/p21869.jpg"
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl =  URL("https://peakbagger.com/climber/ascent.aspx?aid=1761962"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem(
            task = task,
            payload = DatedDocument(
                pageCreationDate = null,
                document = loadHtml("peakbagger/TripReportWithImage.html")
            )
        )

        val expectedDiscovery = ImageDiscovery(
            source = expectedImageSrc,
            referencingURL = task.details.entityUrl,
            description = "",
            type = ImageDiscoveryType.THUMBNAIL
        )


        val result = step.process(item, monitor)

        verify(linkDiscoveryService).submitImages(eq(task), org.mockito.kotlin.check {
            assertEquals(1, it.size)
            assertEquals(it.first(), expectedDiscovery)
        })

        assertTrue(result.continueProcessing)
    }
}