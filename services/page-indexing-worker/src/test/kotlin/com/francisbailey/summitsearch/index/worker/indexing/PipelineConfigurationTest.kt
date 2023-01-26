package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.configuration.PipelineConfiguration
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.sksamuel.scrimage.ImmutableImage
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL
import java.time.Duration
import java.util.*

class PipelineConfigurationTest: StepTest() {

    private val fetchHtmlPageStep = mock<FetchHtmlPageStep>()
    private val submitLinksStep = mock<SubmitLinksStep>()
    private val indexHtmlPageStep = mock<IndexHtmlPageStep>()
    private val fetchImageStep = mock<FetchImageStep>()
    private val generateThumbnailStep = mock<GenerateThumbnailStep>()
    private val saveThumbnailStep = mock<SaveThumbnailStep>()
    private val submitThumbnailStep = mock<SubmitThumbnailStep>()

    private val pipelineConfiguration = PipelineConfiguration(
        fetchHtmlPageStep = fetchHtmlPageStep,
        submitLinksStep = submitLinksStep,
        indexHtmlPageStep = indexHtmlPageStep,
        fetchImageStep = fetchImageStep,
        generateThumbnailStep = generateThumbnailStep,
        saveThumbnailStep = saveThumbnailStep,
        submitThumbnailStep = submitThumbnailStep
    )

    @Test
    fun `executes steps in expected order for html route`() {
        val task = IndexTask(
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

        val pipelineItem = PipelineItem<Document>(task = task, payload = null)

        whenever(fetchHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitLinksStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitThumbnailStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchHtmlPageStep, submitLinksStep, indexHtmlPageStep, submitThumbnailStep) {
            verify(fetchHtmlPageStep).process(any(), any())
            verify(submitLinksStep).process(any(), any())
            verify(indexHtmlPageStep).process(any(), any())
            verify(submitThumbnailStep).process(any(), any())
        }
    }

    @Test
    fun `executes steps in expected order for thumbnail route`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.THUMBNAIL,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = PipelineItem<ImmutableImage>(task = task, payload = null)

        whenever(fetchImageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(generateThumbnailStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(saveThumbnailStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchImageStep, generateThumbnailStep, saveThumbnailStep) {
            verify(fetchImageStep).process(any(), any())
            verify(generateThumbnailStep).process(any(), any())
            verify(saveThumbnailStep).process(any(), any())
        }
    }
}