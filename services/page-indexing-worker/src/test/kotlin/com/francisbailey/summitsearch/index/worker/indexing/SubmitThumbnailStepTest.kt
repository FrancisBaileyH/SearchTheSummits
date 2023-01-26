package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.ImageTaskContext
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import com.francisbailey.summitsearch.index.worker.extension.getOGImage
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitThumbnailStep
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SubmitThumbnailStepTest: StepTest() {


    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()

    private val step = SubmitThumbnailStep(indexingTaskQueueClient)

    @Test
    fun `does not submit task if thumbnail is not present`() {
        val document = mock<Document>()
        whenever(document.getOGImage()).thenReturn(null)

        val item = PipelineItem(
            payload = document,
            task = defaultIndexTask
        )

        step.process(item, monitor)

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

        val item = PipelineItem(
            payload = document,
            task = defaultIndexTask
        )

        step.process(item, monitor)

        verify(indexingTaskQueueClient).addTask(org.mockito.kotlin.check {
            assertEquals(image.imageSrc, it.details.pageUrl.toString())
            val context = it.details.getContext<ImageTaskContext>()

            assertEquals(defaultIndexTask.details.pageUrl, context!!.referencingURL)
            assertEquals(image.caption, context.description)
        })
    }
}