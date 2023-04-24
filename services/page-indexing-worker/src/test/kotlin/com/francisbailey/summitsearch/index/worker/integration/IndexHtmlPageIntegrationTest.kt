package com.francisbailey.summitsearch.index.worker.integration

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.configuration.ExtractorConfiguration
import com.francisbailey.summitsearch.index.worker.configuration.IndexConfiguration
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.indexservice.DocumentQueryRequest
import com.francisbailey.summitsearch.indexservice.test.ElasticSearchTestServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class IndexHtmlPageIntegrationTest: StepTest() {

    private val documentFilterService = mock<DocumentFilterService>()

    private val indexConfiguration = IndexConfiguration(client)

    private val summitSearchIndex = indexConfiguration.summitSearchIndexService()

    private val extractor = ExtractorConfiguration(mock()).htmlContentExtractor()

    private val step = IndexHtmlPageStep(
        documentFilterService,
        summitSearchIndex,
        extractor
    )

    @Test
    @Tag("integration")
    fun `query term lily returns exactly one result with expected source and search highlight`() {
        val item = buildPipelineItem(
            htmlPage = "LilyRock.html",
            url = URL("https://example.com/lilyrock")
        )

        step.process(item, monitor)

        refreshIndex()

        val results = summitSearchIndex.query(DocumentQueryRequest(term = "Lily"))

        Assertions.assertEquals(1, results.hits.size)

        val result = results.hits.first()

        Assertions.assertEquals(
            "<em>Lily</em> Rock is by no doubt one of the most historically significant summits in North America for climbers and mountaineers alike. It's origins start back in the",
            result.highlight
        )
        Assertions.assertEquals(item.task.details.entityUrl.toString(), result.source)
        Assertions.assertEquals("Lily Peak via White Maiden's Walkway", result.title)
    }

    @Test
    @Tag("integration")
    fun `query term texas peak returns expected source and search highlight`() {
        val item = buildPipelineItem(
            htmlPage = "TexasPeak.html",
            url = URL("https://example.com/texaspeak")
        )

        step.process(item, monitor)

        refreshIndex()

        val results = summitSearchIndex.query(DocumentQueryRequest(term = "Texas Peak"))

        Assertions.assertEquals(1, results.hits.size)

        val result = results.hits.first()

        Assertions.assertEquals(
            "We only had a few days and needed an easy outing, so my family suggested a popular local summit called <em>Texas</em> <em>Peak</em>.",
            result.highlight
        )
        Assertions.assertEquals(item.task.details.entityUrl.toString(), result.source)
        Assertions.assertEquals("Texas Peak via Stenson Creek FSR", result.title)
    }

    @Test
    @Tag("integration")
    fun `page with div only text is indexed and searchable`() {
        val item = buildPipelineItem(
            htmlPage = "ContentWithoutParagraphs.html",
            url = URL("https://example.com/ContentWithoutParagraphs")
        )

        step.process(item, monitor)

        refreshIndex()

        val results = summitSearchIndex.query(DocumentQueryRequest(term = "Connaught"))

        Assertions.assertEquals(1, results.hits.size)

        val result = results.hits.first()

        Assertions.assertEquals(
            "We had traversed so far to the right that we had reached the main uptrack leading into the <em>Connaught</em> Drainage. We couldn't believe it.",
            result.highlight
        )
        Assertions.assertEquals(item.task.details.entityUrl.toString(), result.source)
        Assertions.assertEquals(
            "The blog formerly known as Trip Reports from the Okanagan: Little Sifton Col and Puff Daddy",
            result.title
        )
    }

    @Test
    @Tag("integration")
    fun `pages without paragraphs or text are excluded`() {
        val pages = setOf("NoParagraphs.html", "TwinsTower.html")

        pages.forEach {
            step.process(buildPipelineItem(it, URL("https://example.com/$it")), monitor)
        }

        refreshIndex()

        val result = summitSearchIndex.query(DocumentQueryRequest(
            term = "Twins Tower"
        ))

        Assertions.assertEquals(1, result.hits.size)
        Assertions.assertEquals("https://example.com/TwinsTower.html", result.hits.first().source)
        Assertions.assertEquals(
            "Like Bill Corbett mentioned in this The 11,000ers book, \"the sudden view of <em>Twins</em> <em>Tower</em> from…",
            result.hits.first().highlight
        )
        Assertions.assertEquals("Twins Tower | Steven's Peak-bagging Journey", result.hits.first().title)
    }

    @Test
    @Tag("integration")
    fun `excluded elements are removed from page on example site CascadeClimbers`() {
        val item = buildPipelineItem(
            htmlPage = "AssiniboineCC.html",
            url = URL("https://example.com/AssiniboineCC")
        )

        step.process(item, monitor)

        refreshIndex()

        val result = summitSearchIndex.query(DocumentQueryRequest(
            term = "Share this post"
        ))

        Assertions.assertEquals(0, result.hits.size)
    }

    private fun buildPipelineItem(htmlPage: String, url: URL): PipelineItem<DatedDocument> {
        return PipelineItem(
            task = IndexTask(
                messageHandle = "testHandle123",
                source = "some-queue-name",
                details = IndexTaskDetails(
                    id = "123456",
                    entityUrl =  url,
                    submitTime = Date().time,
                    taskRunId = "test123",
                    taskType = IndexTaskType.HTML,
                    entityTtl = Duration.ofMinutes(60).seconds
                )
            ),
            payload = DatedDocument(
                pageCreationDate = LocalDateTime.now(),
                document = loadHtml(htmlPage)
            )
        )
    }

    private fun refreshIndex() {
        client.indices().refresh()
    }

    companion object {
        private val testServer = ElasticSearchTestServer.global()

        private val client = testServer.client()
    }
}