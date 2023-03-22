package com.francisbailey.summitsearch.index.worker.indexing.override

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.override.SkiSicknessSubmitLinksStep
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SkiSicknessSubmitLinksStepTest: StepTest() {

    private val linkDiscoveryService = mock<LinkDiscoveryService>()

    private val step = SkiSicknessSubmitLinksStep(linkDiscoveryService)

    @Test
    fun `submits all links to link discovery service`() {
        val links = listOf(
            "https://skisickness.com/post/vt612-pandemic-pandemonium",
            "https://skisickness.com/post/vt617-mt-despair-south-face-and-triumph-environs?&p=3498#p3498",
            "https://skisickness.com/post/vt605-outram-west-face?sid=04ebb535672d5e7d35069c5e2dda5bd9"
        )

        val expectedLinks = listOf(
            "https://skisickness.com/post/vt612-pandemic-pandemonium",
            "https://skisickness.com/post/vt617-mt-despair-south-face-and-triumph-environs?&p=3498#p3498",
            "https://skisickness.com/post/vt605-outram-west-face"
        ).map {
            Discovery(IndexTaskType.HTML, it)
        }

        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        links.forEach {
            htmlContent.body().appendElement("a")
                .attr("href", it)
                .text(it)
        }

        val item = PipelineItem<DatedDocument>(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = htmlContent
            )
        )

        val result = step.process(item, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)
        verify(linkDiscoveryService).submitDiscoveries(defaultIndexTask, expectedLinks)
    }

}