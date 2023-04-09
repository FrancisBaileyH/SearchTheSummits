package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexFacebookPostStep
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.URL
import java.time.Duration
import java.util.*

class IndexFacebookPostStepTest: StepTest() {

    private val index = mock<DocumentIndexService>()

    private val step = IndexFacebookPostStep(index)

    @Test
    fun `indexes expected content from facebook page`() {
        val expectedParagraphContent = "Mount Hansen Northwest Peak. I took a chance on the Sowerby Creek FSR hoping I could drive right on to the ridge... that was a hard no. The road deteriorates quickly and becomes atv terrain, so the road hiking began. 2.5hrs later I started making my way through the forest going over a few bumps and staying close to the center of the ridge, I eventually found a flagged route and followed that the rest of the way. After 14kms in I ended my hike at the sub Summit(Northwest peak) of Hansen and sent the drone to do the dirty work. It's probably no more then 30-40mins to the true Summit but I was done with it lol. Just over 28kms roundtrip, 1350m gain approx, 9.5hrs"
        val expectedSeoDescription = "Mount Hansen Northwest Peak. I took a chance on the Sowerby Creek FSR hoping I could drive right on to the ridge... that was a hard no. The road deteriorates quickly and becomes atv terrain, so the..."
        val document = loadHtml("facebook/facebook-group-post.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl =  URL("https://www.facebook.com/groups/240069949775049/posts/1183280815453953/"),
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

        verify(index).indexContent(DocumentPutRequest(
            source = task.details.entityUrl,
            title = "SWBC Peak Baggers | Mount Hansen Northwest Peak",
            rawTextContent = "",
            paragraphContent = expectedParagraphContent,
            seoDescription = expectedSeoDescription,
            pageCreationDate = null
        ))
    }

}