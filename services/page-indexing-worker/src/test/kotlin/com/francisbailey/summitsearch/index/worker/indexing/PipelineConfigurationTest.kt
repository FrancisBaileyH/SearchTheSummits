package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.configuration.PipelineConfiguration
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import com.sksamuel.scrimage.ImmutableImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
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
    private val thumbnailValidationStep = mock<ThumbnailValidationStep>()
    private val contentValidatorStep = mock<ContentValidatorStep>()
    private val peakBaggerContentValidatorStep = mock<PeakBaggerContentValidatorStep>()
    private val peakBaggerSubmitThumbnailStep = mock<PeakBaggerSubmitThumbnailStep>()
    private val fetchPDFStep = mock<FetchPDFStep>()
    private val indexPDFStep = mock<IndexPDFStep>()
    private val closePDFStep = mock<ClosePDFStep>()

    private val pipelineConfiguration = PipelineConfiguration(
        fetchHtmlPageStep = fetchHtmlPageStep,
        submitLinksStep = submitLinksStep,
        indexHtmlPageStep = indexHtmlPageStep,
        fetchImageStep = fetchImageStep,
        generateThumbnailStep = generateThumbnailStep,
        saveThumbnailStep = saveThumbnailStep,
        submitThumbnailStep = submitThumbnailStep,
        thumbnailValidationStep = thumbnailValidationStep,
        contentValidatorStep = contentValidatorStep,
        fetchPDFStep = fetchPDFStep,
        indexPDFStep = indexPDFStep,
        closePDFStep = closePDFStep,
        peakBaggerSubmitThumbnailStep = peakBaggerSubmitThumbnailStep,
        peakBaggerContentValidatorStep = peakBaggerContentValidatorStep
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

        val pipelineItem = PipelineItem<DatedDocument>(task = task, payload = null)

        whenever(fetchHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitLinksStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitThumbnailStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(contentValidatorStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchHtmlPageStep, submitLinksStep, indexHtmlPageStep, submitThumbnailStep, contentValidatorStep) {
            verify(fetchHtmlPageStep).process(any(), any())
            verify(submitLinksStep).process(any(), any())
            verify(contentValidatorStep).process(any(), any())
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
        whenever(thumbnailValidationStep.process(any(), any())).thenReturn(pipelineItem)


        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchImageStep, generateThumbnailStep, saveThumbnailStep, thumbnailValidationStep) {
            verify(thumbnailValidationStep).process(any(), any())
            verify(fetchImageStep).process(any(), any())
            verify(generateThumbnailStep).process(any(), any())
            verify(saveThumbnailStep).process(any(), any())
        }
    }

    @Test
    fun `executes steps in expected order for pdf route`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.PDF,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = PipelineItem<PDDocument>(task = task, payload = null)

        whenever(fetchPDFStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexPDFStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(closePDFStep.process(any(), any())).thenReturn(pipelineItem)


        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchPDFStep, indexPDFStep, closePDFStep) {
            verify(fetchPDFStep).process(any(), any())
            verify(indexPDFStep).process(any(), any())
            verify(closePDFStep).process(any(), any())
        }
    }

    @Test
    fun `overrides on steps for peakbagger`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = URL("https://peakbagger.com/climbers/ascent.aspx?aid=12"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = PipelineItem<DatedDocument>(task = task, payload = null)

        whenever(fetchHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitLinksStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(peakBaggerSubmitThumbnailStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(peakBaggerContentValidatorStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchHtmlPageStep, indexHtmlPageStep, submitLinksStep, peakBaggerContentValidatorStep, peakBaggerSubmitThumbnailStep) {
            verify(fetchHtmlPageStep).process(any(), any())
            verify(submitLinksStep).process(any(), any())
            verify(peakBaggerContentValidatorStep).process(any(), any())
            verify(indexHtmlPageStep).process(any(), any())
            verify(peakBaggerSubmitThumbnailStep).process(any(), any())
        }

        verifyNoInteractions(submitThumbnailStep)
        verifyNoInteractions(contentValidatorStep)
    }
}