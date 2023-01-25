package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitLinksStep
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SubmitLinksStepTest: StepTest() {

    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = SubmitLinksStep(linkDiscoveryService)

    @Test
    fun `submits all links to link discovery service`() {
        val links = listOf("https://francisbailey.com/test", "https://francisbailey.com/test2")
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        links.forEach {
            htmlContent.body().appendElement("a")
                .attr("href", it)
                .text(it)
        }

        val item = PipelineItem<Document>(
            task = defaultIndexTask,
            payload = htmlContent
        )

        val result = step.process(item, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.canRetry)
        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, links)
    }

}