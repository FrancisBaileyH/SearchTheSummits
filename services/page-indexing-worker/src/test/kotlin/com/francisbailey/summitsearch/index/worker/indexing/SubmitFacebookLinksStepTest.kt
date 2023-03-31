package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitFacebookLinksStep
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.URL
import java.time.Duration
import java.util.*

class SubmitFacebookLinksStepTest: StepTest() {

    private val discoveryService = mock<LinkDiscoveryService>()

    private val step = SubmitFacebookLinksStep(discoveryService)

    @Test
    fun `indexes expected content from facebook page`() {
        val document = loadHtml("facebook/facebook-group-feed.html")
        val expectedLinks = listOf(
            "https://www.facebook.com/groups/240069949775049/posts/1565897663858931/",
            "https://www.facebook.com/groups/240069949775049/posts/1510369772745054/",
            "https://www.facebook.com/groups/240069949775049/posts/1503218993460132/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/",
            "https://www.facebook.com/groups/240069949775049/posts/1617945431987487/"
        )

        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl = URL("https://www.facebook.com/groups/240069949775049/"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem(
            payload = DatedDocument(
                document = document,
                pageCreationDate = null
            ),
            task = task
        )

        step.process(item, monitor)

        verify(discoveryService).submitDiscoveries(eq(task), org.mockito.kotlin.check {
            assertEquals(expectedLinks, it.map { discovery -> discovery.source })
        })
    }
}