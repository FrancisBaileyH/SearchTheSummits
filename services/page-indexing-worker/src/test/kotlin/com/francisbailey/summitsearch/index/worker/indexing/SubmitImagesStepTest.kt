package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitImagesStep
import com.francisbailey.summitsearch.index.worker.task.ImageDiscovery
import com.francisbailey.summitsearch.index.worker.task.ImageDiscoveryType
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.ZoneOffset

class SubmitImagesStepTest: StepTest() {

    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = SubmitImagesStep(linkDiscoveryService)

    @Test
    fun `submits expected images to link discovery service`() {
        val html = """
                <figure>
                    <img id="good-image" src="a-good-source.png" />
                    <figcaption>Hello world</figcaption>
                </figure>
                <figure>
                    <img id="bad-image-without-caption" src="some-other-source" />
                </figure>
                <div class="wp-caption aligncenter">
                    <a href="#"><img id="good-image" src="another-good-source.png" /></a>
                    <p class="wp-caption-text">Hello world 2</p>
                </div>
        """.trimIndent()

        val document = DatedDocument(
            document = Jsoup.parse(html),
            pageCreationDate = LocalDateTime.now()
        )

        val expectedDiscoveries = setOf(
            ImageDiscovery(
                source = "a-good-source.png",
                referencingURL = defaultIndexTask.details.entityUrl,
                description = "Hello world",
                pageCreationDate = document.pageCreationDate!!.toInstant(ZoneOffset.UTC).toEpochMilli(),
                type = ImageDiscoveryType.STANDARD,
                referencingTitle = ""
            ),
            ImageDiscovery(
                source = "another-good-source.png",
                referencingURL = defaultIndexTask.details.entityUrl,
                description = "Hello world 2",
                pageCreationDate = document.pageCreationDate!!.toInstant(ZoneOffset.UTC).toEpochMilli(),
                type = ImageDiscoveryType.STANDARD,
                referencingTitle = ""
            )
        )

        val item = PipelineItem(task = defaultIndexTask, payload = document)

        val result = step.process(item, monitor)

        verify(linkDiscoveryService).submitImages(eq(defaultIndexTask), check {
            assertEquals(2, it.size)
            assertTrue(it.containsAll(expectedDiscoveries))
        })

        assertTrue(result.continueProcessing)
    }

}