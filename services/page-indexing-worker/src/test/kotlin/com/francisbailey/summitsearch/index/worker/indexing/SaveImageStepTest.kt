package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.SaveImageStep
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.*

class SaveImageStepTest: StepTest() {

    private val image = mock<ImmutableImage> {
//        on(mock.height).thenReturn(120)
//        on(mock.width).thenReturn(200)
    }

    private val imageStore = mock<ImageWriterStore>()

    private val index = mock<ImageIndexService>()

    private val imageWriter = mock<PngWriter>()

    private val step = SaveImageStep(imageStore, index, imageWriter)


    @Test
    fun `saves image to store and then index`() {
        val imageData = ByteArray(1)
        val imageCaption = "This is an image"
        val imageSrc = URL("https://www.test.com/some/path/here/image.jpeg")

        val referenceStoreUrl = URL("https://www.test-store.com/abc1234")

        val context = ImageTaskContext(
            referencingURL = URL("https://www.francisbaileyh.com"),
            description = imageCaption,
            pageCreationDate = Instant.now().toEpochMilli()
        )

        val task = IndexTask(
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = imageSrc,
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.IMAGE,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                context = Json.encodeToString(context)
            )
        )

        val item = PipelineItem(
            payload = image,
            task = task
        )

        whenever(image.bytes(any())).thenReturn(imageData)
        whenever(imageStore.save(any<URL>(), any(), any())).thenReturn(referenceStoreUrl)

        step.process(item, monitor)

        verify(imageStore).save(imageSrc, imageData, ImageStoreType.STANDARD)
        verify(index).indexImage(org.mockito.kotlin.check {
            assertEquals(context.referencingURL, it.referencingDocument)
            assertEquals(context.description, it.description)
            assertEquals(task.details.pageUrl, it.source)
            assertEquals(context.pageCreationDate, it.referencingDocumentDate)
            assertEquals(referenceStoreUrl.toString(), it.dataStoreReference)
            assertEquals(0, it.heightPx) // this is not super ideal, but can't mock final properties
            assertEquals(0, it.widthPx)  // for now just rely on the mocks default of 0...
        })

    }

    @Test
    fun `normalizes image names for IDs`() {
        val imageData = ByteArray(1)
        val imageCaption = "This is an image"
        val imageSrc = URL("https://www.test.com/some/path/here/image.jpeg?w=124&h=12312")

        val referenceStoreUrl = URL("https://www.test-store.com/abc1234")

        val context = ImageTaskContext(
            referencingURL = URL("https://www.francisbaileyh.com"),
            description = imageCaption,
            pageCreationDate = Instant.now().toEpochMilli()
        )

        val task = IndexTask(
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = imageSrc,
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.IMAGE,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                context = Json.encodeToString(context)
            )
        )

        val item = PipelineItem(
            payload = image,
            task = task
        )

        whenever(image.bytes(any())).thenReturn(imageData)
        whenever(imageStore.save(any<URL>(), any(), any())).thenReturn(referenceStoreUrl)

        step.process(item, monitor)

        verify(imageStore).save(URL("https://www.test.com/some/path/here/image.jpeg"), imageData, ImageStoreType.STANDARD)
        verify(index).indexImage(org.mockito.kotlin.check {
            assertEquals(context.referencingURL, it.referencingDocument)
            assertEquals(context.description, it.description)
            assertEquals(task.details.pageUrl, it.source)
            assertEquals(context.pageCreationDate, it.referencingDocumentDate)
            assertEquals(referenceStoreUrl.toString(), it.dataStoreReference)
            assertEquals(URL("https://www.test.com/some/path/here/image.jpeg"), it.normalizedSource)
        })

    }

}