package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.configuration.PipelineConfiguration
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.sksamuel.scrimage.ImmutableImage
import org.apache.pdfbox.pdmodel.PDDocument
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
    private val fetchPDFStep = mock<FetchPDFStep>()
    private val indexPDFStep = mock<IndexPDFStep>()
    private val closePDFStep = mock<ClosePDFStep>()
    private val submitImagesStep = mock<SubmitImagesStep>()
    private val saveImageStep = mock<SaveImageStep>()
    private val generateImagePreviewStep = mock<GenerateImagePreviewStep>()
    private val checkImageExistsStep = mock<CheckImageExistsStep>()

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
        generateImagePreviewStep = generateImagePreviewStep,
        submitImagesStep = submitImagesStep,
        saveImageStep = saveImageStep,
        checkImageExistsStep = checkImageExistsStep,
        htmlProcessingOverrides = emptyMap()
    )

    @Test
    fun `executes steps in expected order for html route`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                entityUrl =  URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = mock<PipelineItem<DatedDocument>> {
            on(mock.task).thenReturn(task)
            on(mock.continueProcessing).thenReturn(true)
        }

        whenever(fetchHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitLinksStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitThumbnailStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(contentValidatorStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitImagesStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchHtmlPageStep, submitLinksStep, indexHtmlPageStep, submitThumbnailStep, contentValidatorStep, submitImagesStep) {
            verify(fetchHtmlPageStep).process(any(), any())
            verify(submitLinksStep).process(any(), any())
            verify(contentValidatorStep).process(any(), any())
            verify(submitImagesStep).process(any(), any())
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
                entityUrl =  URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.THUMBNAIL,
                entityTtl = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = mock<PipelineItem<ImmutableImage>> {
            on(mock.task).thenReturn(task)
            on(mock.continueProcessing).thenReturn(true)
        }

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
    fun `executes steps in expected order for image route`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                entityUrl =  URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.IMAGE,
                entityTtl = Duration.ofMinutes(60).seconds
            )
        )

        val pipelineItem = mock<PipelineItem<ImmutableImage>> {
            on(mock.task).thenReturn(task)
            on(mock.continueProcessing).thenReturn(true)
        }

        whenever(fetchImageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(generateImagePreviewStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(saveImageStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchImageStep, generateImagePreviewStep, saveImageStep) {
            verify(fetchImageStep).process(any(), any())
            verify(generateImagePreviewStep).process(any(), any())
            verify(saveImageStep).process(any(), any())
        }
    }

    @Test
    fun `executes steps in expected order for pdf route`() {
        val task = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                entityUrl =  URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.PDF,
                entityTtl = Duration.ofMinutes(60).seconds
            )
        )


        val pipelineItem = mock<PipelineItem<PDDocument>> {
            on(mock.task).thenReturn(task)
            on(mock.continueProcessing).thenReturn(true)
        }

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
}