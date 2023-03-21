package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitThumbnailStep
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class SubmitThumbnailStepTest: StepTest() {

    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()

    private val step = SubmitThumbnailStep(indexingTaskQueueClient)

    @Test
    fun `does not submit task if thumbnail is not present`() {
        val document = Jsoup.parse("<html><body></body></html>")

        val item = PipelineItem<DatedDocument>(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = document
            )
        )

        val result = step.process(item, monitor)

        assertTrue(result.continueProcessing)
        verifyNoInteractions(indexingTaskQueueClient)
    }

    @Test
    fun `submits task if thumbnail is present`() {
        val image = CaptionedImage(
            imageSrc = "https://test.com/image.jpeg",
            caption = "Test"
        )

        val html = """
            <html>
            <head>
            <meta property="og:image" content="${image.imageSrc}" />
            <meta property="og:image:alt" content="${image.caption}" />
            </head>
            <body>
                <p>YEP</p>
            </body>
            </html>
        """

        val document = Jsoup.parse(html)

        val item = PipelineItem<DatedDocument>(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = document
            )
        )

        val result = step.process(item, monitor)

        verify(indexingTaskQueueClient).addTask(org.mockito.kotlin.check {
            assertEquals(image.imageSrc, it.details.entityUrl.toString())
            val context = it.details.getContext<ImageTaskContext>()

            assertEquals(defaultIndexTask.details.entityUrl, context!!.referencingURL)
            assertEquals(image.caption, context.description)
        })
        assertTrue(result.continueProcessing)
    }
}