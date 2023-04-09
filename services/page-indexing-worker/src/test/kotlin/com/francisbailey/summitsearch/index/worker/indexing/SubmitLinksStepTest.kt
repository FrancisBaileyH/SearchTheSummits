package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitLinksStep
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SubmitLinksStepTest: StepTest() {

    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = SubmitLinksStep(linkDiscoveryService)

    @Test
    fun `submits all links to link discovery service`() {
        val links = listOf("https://francisbailey.com/test", "https://francisbailey.com/test2").map {
            Discovery(IndexTaskType.HTML, it)
        }
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        links.forEach {
            htmlContent.body().appendElement("a")
                .attr("href", it.source)
                .text(it.source)
        }

        val item = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = htmlContent
            )
        )

        val result = step.process(item, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)
        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, links)
    }

    @Test
    fun `sets task type to PDF if content ends with PDF`() {
        val expectedLinks = listOf(
            Discovery(IndexTaskType.HTML, "https://francisbailey.com/test"),
            Discovery(IndexTaskType.PDF, "https://francisbailey.com/test.pDf")
        )

        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        expectedLinks.forEach {
            htmlContent.body().appendElement("a")
                .attr("href", it.source)
                .text(it.source)
        }

        val item = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = htmlContent
            )
        )

        val result = step.process(item, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)
        verify(linkDiscoveryService).submitDiscoveries(any(), org.mockito.kotlin.check {
            assertEquals(expectedLinks.first(), it.first())
            assertEquals(expectedLinks.component2(), it.component2())
        })
    }

    @Test
    fun `submits embedded pdf links + normal links`() {
        val html = """
            <html>
                <body>
                    <div>
                        <a href="https://example.com">Some Link</a>
                        <object data="https://bcmc.ca/media/newsletters/BCMC Newsletter 2001-11" type="application/pdf" width="640" height="800" style="border:1px solid black;"></object>
                    </div>
                </body>
           </html>
        """

        val expectedLinks = listOf(
            Discovery(IndexTaskType.HTML, "https://example.com"),
            Discovery(IndexTaskType.PDF, "https://bcmc.ca/media/newsletters/BCMC Newsletter 2001-11")
        )

        val item = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse(html)
            )
        )

        val result = step.process(item, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)
        
        verify(linkDiscoveryService).submitDiscoveries(any(), org.mockito.kotlin.check {
            assertEquals(expectedLinks.first(), it.first())
            assertEquals(expectedLinks.component2(), it.component2())
        })
    }

}