package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.SaveThumbnailStep
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URL
import java.time.Duration
import java.util.*

class SaveThumbnailStepTest: StepTest() {

    private val image = mock<ImmutableImage>()

    private val imageStore = mock<ImageWriterStore>()

    private val index = mock<DocumentIndexService>()

    private val imageWriter = mock<PngWriter>()

    private val step = SaveThumbnailStep(imageStore, imageWriter, index)


    @Test
    fun `saves thumbnail to store and then index`() {

        val imageData = ByteArray(1)
        val imageCaption = "This is an image"
        val imageSrc = URL("https://www.test.com/some/path/here/image.jpeg")

        val referenceStoreUrl = URL("https://www.test-store.com/abc1234")

        val context = ImageTaskContext(
            referencingURL = URL("https://www.francisbaileyh.com"),
            description = imageCaption
        )

        val task = IndexTask(
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                entityUrl =  imageSrc,
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                context = Json.encodeToString(context)
            )
        )

        val item = PipelineItem(
            payload = image,
            task = task
        )

        whenever(image.bytes(any())).thenReturn(imageData)
        whenever(imageStore.save(any<URL>(), any(), any())).thenReturn(referenceStoreUrl)

        val result = step.process(item, monitor)

        verify(imageStore).save(imageSrc, imageData, ImageStoreType.THUMBNAIL)
        verify(index).putThumbnails(org.mockito.kotlin.check {
            assertEquals(context.referencingURL, it.source)
            assertEquals(listOf(referenceStoreUrl.toString()), it.dataStoreReferences)
        })
        assertTrue(result.continueProcessing)
    }

}