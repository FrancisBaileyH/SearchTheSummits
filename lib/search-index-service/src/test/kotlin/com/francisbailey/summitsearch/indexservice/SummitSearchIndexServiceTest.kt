package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch._types.Refresh
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.GetIndexRequest
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.URL

class SummitSearchIndexServiceTest {

    private val testServer = ElasticSearchTestServer.global()

    private val client = testServer.client()

    private val resources = File("src/test/resources")

    private val indexedPages = setOf("LibertyBell", "LilyRock", "TexasPeak", "ContentWithoutParagraphs")

    private val source = "https://www.francisbaileyh.com"


    @Test
    fun `returns false if index does not exist and true when it does`() {
        val index = "non-existent-index"
        val testIndexService = SummitSearchIndexService(client, 10, index)

        assertFalse(testIndexService.indexExists())

        client.indices().create(CreateIndexRequest.of {
            it.index(index)
        })

        assertTrue(testIndexService.indexExists())
    }

    @Test
    fun `creates index with html analyzer if index does not exist yet`() {
        val index = "create-index-not-exist"
        val testIndexService = SummitSearchIndexService(client, 10, index)
        assertFalse(testIndexService.indexExists())

        testIndexService.createIndexIfNotExists()

        assertTrue(testIndexService.indexExists())
    }

    @Test
    fun `query throws IllegalArgumentException when from value exceeds maximum`() {
        val testIndexService = createIndex("test")

        assertThrows<IllegalArgumentException> {
            testIndexService.query(SummitSearchQueryRequest(term = "test", from = SummitSearchIndexService.MAX_FROM_VALUE + 1))
        }
    }

    @Test
    fun `query throws IllegalArgumentException when max term size exceeded`() {
        val testIndexService = createIndex("test")

        val term = StringBuffer().apply {
            repeat(SummitSearchIndexService.MAX_QUERY_TERM_SIZE + 1) {
                this.append("a")
            }
        }.toString()

        assertThrows<IllegalArgumentException> {
            testIndexService.query(SummitSearchQueryRequest(term = term, from = 1))
        }
    }

    @Test
    fun `query term lily returns exactly one result with expected source and search highlight`() {
        val index = "test-index-lily"
        val testIndexService = createIndex(index)
        indexHtmlContent(index)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Lily"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("As it turned out, a permit was only required for the hiking trails up to Tahquitz Peak, but <em>Lily</em> rock", result.highlight)
        assertEquals("$source/LilyRock", result.source)
        assertEquals("Lily Peak via White Maiden's Walkway", result.title)
    }

    @Test
    fun `query term texas peak returns expected source and search highlight`() {
        val index = "test-index-texas"
        val testIndexService = createIndex(index)
        indexHtmlContent(index)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Texas Peak"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("Still a trail visible as you can see Looking towards <em>Texas</em> <em>Peak</em> (true summit) from West <em>Texas</em> <em>Peak</em> (false", result.highlight)
        assertEquals("$source/TexasPeak", result.source)
        assertEquals("Texas Peak via Stenson Creek FSR", result.title)
    }

    @Test
    fun `page with div only text is indexed and searchable`() {
        val index = "test-index-div-paragraph"
        val testIndexService = createIndex(index)
        indexHtmlContent(index)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Connaught"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("We had traversed so far to the right that we had reached the main uptrack leading into the <em>Connaught</em>", result.highlight)
        assertEquals("$source/ContentWithoutParagraphs", result.source)
        assertEquals("The blog formerly known as Trip Reports from the Okanagan: Little Sifton Col and Puff Daddy", result.title)
    }

    @Test
    fun `query is paginated across multiple requests`() {
        val index = "test-index-peak"
        val testIndexService = createIndex(index, pagination = 1)
        indexHtmlContent(index)

        val paginatedResults = generateSequence(testIndexService.query(SummitSearchQueryRequest(term = "Peak"))) {
            if (it.hits.isNotEmpty()) {
                testIndexService.query(SummitSearchQueryRequest(term = "Peak", from = it.next))
            } else {
                null
            }
        }.takeWhile {
            it.hits.isNotEmpty()
        }.toList()

        assertEquals(4,  paginatedResults.size)

        val sources = indexedPages.map { "$source/$it" }.toSet()

        paginatedResults.forEach {
            assertTrue(sources.contains(it.hits.first().source))
            assertEquals(4, it.totalHits)
        }
    }

    @Test
    fun `indexing page content can be searched after`() {
        val index = "test-index-liberty"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")

        assertTrue(testIndexService.query(SummitSearchQueryRequest(term = "Liberty")).hits.isEmpty())
        testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$source/LibertyBell"), htmlDocument = page))
        refreshIndex(index)


        assertFalse(testIndexService.query(SummitSearchQueryRequest(term = "Liberty")).hits.isEmpty())
    }

    @Test
    fun `pages without paragraphs or text are excluded`() {
        val index = "index-with-paragraph-free-text"
        val pages = setOf("NoParagraphs", "TwinsTower")

        val testIndexService = createIndex(index)

        pages.forEach {
            testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$source/$it"), htmlDocument = loadHtml(it)))
        }

        refreshIndex(index)

        val result = testIndexService.query(SummitSearchQueryRequest(
            term = "Twins Tower"
        ))

        assertEquals(1, result.hits.size)
        assertEquals("$source/TwinsTower", result.hits.first().source)
        assertEquals("Down-climbing <em>Twins</em> <em>Tower</em> Looking over the face towards Mt. Alberta And, here’s the exposure!!", result.hits.first().highlight)
        assertEquals("Twins Tower | Steven's Peak-bagging Journey", result.hits.first().title)
    }

    @Test
    fun `excluded elements are removed from page on example site CascadeClimbers`() {
        val index = "index-with-excluded-elements"
        val pages = setOf("AssiniboineCC")

        val testIndexService = createIndex(index)

        pages.forEach {
            testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$source/$it"), htmlDocument = loadHtml(it)))
        }

        refreshIndex(index)

        val result = testIndexService.query(SummitSearchQueryRequest(
            term = "Share this post"
        ))

        assertEquals(0, result.hits.size)
    }

    @Test
    fun `deletes document from index when delete call made`() {
        val index = "test-index-delete-test"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")
        val url = URL("$source/LibertyBell")

        testIndexService.indexPageContents(SummitSearchIndexRequest(source = url, htmlDocument = page))
        refreshIndex(index)


        assertEquals(url.toString(), testIndexService.query(SummitSearchQueryRequest(term = "Liberty")).hits.first().source)

        testIndexService.deletePageContents(SummitSearchDeleteIndexRequest(url))
        refreshIndex(index)

        val result = client.get(GetRequest.of {
            it.index(index)
            it.id(url.toString())
        }, HtmlMapping::class.java)

        assertFalse(result.found())
    }

    private fun refreshIndex(index: String) {
        client.indices().refresh(RefreshRequest.of {
            it.index(index)
        })
    }

    private fun createIndex(index: String, pagination: Int = 10): SummitSearchIndexService {
        return SummitSearchIndexService(client, pagination, index).also {
            it.createIndexIfNotExists()
        }
    }

    private fun loadHtml(name: String): Document {
        return Jsoup.parse(resources.resolve("$name.html").absoluteFile.readText())
    }

    private fun indexHtmlContent(indexName: String) {
        indexedPages.forEach {
            val sourceURL = URL("$source/$it")
            val html = loadHtml(it)


             client.index(
                IndexRequest.of { request ->
                    request.index(indexName)
                    request.id(sourceURL.toString())
                    request.document(
                        HtmlMapping(
                            source = sourceURL,
                            textContent = html.body().text(),
                            title = html.title(),
                            host = sourceURL.host
                        )
                    )
                    request.refresh(Refresh.True)
                })
        }
    }


}