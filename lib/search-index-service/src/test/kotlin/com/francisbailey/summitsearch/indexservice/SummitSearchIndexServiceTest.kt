package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.elasticsearch.core.search.HighlightField
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder
import co.elastic.clients.elasticsearch.core.search.HitsMetadata
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.kotlin.check
import java.io.File
import java.net.URL

class SummitSearchIndexServiceTest {

    private val testServer = ElasticSearchTestServer.global()

    private val client = testServer.client()

    private val resources = File("src/test/resources")

    private val indexedPages = setOf("LibertyBell", "LilyRock", "TexasPeak", "ContentWithoutParagraphs")

    private val source = "www.francisbaileyh.com"

    private val sourceUrlString = "https://$source"

    private val mockClient = mock<ElasticsearchClient>()

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
        indexHtmlContent(testIndexService)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Lily"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("<em>Lily</em> Rock is by no doubt one of the most historically significant summits in North America for climbers and mountaineers alike. It's origins start back in the", result.highlight)
        assertEquals("$sourceUrlString/LilyRock", result.source)
        assertEquals("Lily Peak via White Maiden's Walkway", result.title)
    }

    @Test
    fun `query term texas peak returns expected source and search highlight`() {
        val index = "test-index-texas"
        val testIndexService = createIndex(index)
        indexHtmlContent(testIndexService)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Texas Peak"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("We only had a few days and needed an easy outing, so my family suggested a popular local summit called <em>Texas</em> <em>Peak</em>.", result.highlight)
        assertEquals("$sourceUrlString/TexasPeak", result.source)
        assertEquals("Texas Peak via Stenson Creek FSR", result.title)
    }

    @Test
    fun `page with div only text is indexed and searchable`() {
        val index = "test-index-div-paragraph"
        val testIndexService = createIndex(index)
        indexHtmlContent(testIndexService)

        val results = testIndexService.query(SummitSearchQueryRequest(term = "Connaught"))

        assertEquals(1, results.hits.size)

        val result = results.hits.first()

        assertEquals("We had traversed so far to the right that we had reached the main uptrack leading into the <em>Connaught</em> Drainage. We couldn't believe it.", result.highlight)
        assertEquals("$sourceUrlString/ContentWithoutParagraphs", result.source)
        assertEquals("The blog formerly known as Trip Reports from the Okanagan: Little Sifton Col and Puff Daddy", result.title)
    }

    @Test
    fun `query is paginated across multiple requests`() {
        val index = "test-index-peak"
        val testIndexService = createIndex(index, pagination = 1)
        indexHtmlContent(testIndexService)

        val paginatedResults = generateSequence(testIndexService.query(SummitSearchQueryRequest(term = "Peak"))) {
            if (it.hits.isNotEmpty()) {
                testIndexService.query(SummitSearchQueryRequest(term = "Peak", from = it.next))
            } else {
                null
            }
        }.takeWhile {
            it.hits.isNotEmpty()
        }.toList()

        assertEquals(2,  paginatedResults.size)

        val sources = indexedPages.map { "$sourceUrlString/$it" }.toSet()

        paginatedResults.forEach {
            assertTrue(sources.contains(it.hits.first().source))
            assertEquals(2, it.totalHits)
        }
    }

    @Test
    fun `indexing page content can be searched after`() {
        val index = "test-index-liberty"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")

        assertTrue(testIndexService.query(SummitSearchQueryRequest(term = "Liberty")).hits.isEmpty())
        testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$sourceUrlString/LibertyBell"), htmlDocument = page))
        refreshIndex(index)

        val result = testIndexService.query(SummitSearchQueryRequest(term = "Liberty"))

        assertFalse(result.hits.isEmpty())

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id("$source/LibertyBell")
            },
            HtmlMapping::class.java
        )
        assertTrue(document.found())
        assertEquals(URL(sourceUrlString).host, document.source()?.host)
        assertEquals("In fact, there's a very high concentration of", document.source()?.seoDescription)
    }

    @Test
    fun `malicious text stripped from content fields on indexing`() {
        val source = "francisbaileyh.com/this-test"
        val sourceUrl = URL("http://$source")
        val maliciousHtml = """
            <html>
                <head>
                    <meta name="description" content="<script>alert('test')/</script>Hello <center>World!</center>">
                </head>
                <body>
                    <p>There is some content here.</p>
                    <p>Here <script>alert('test');</script>too.</p>
                    <div>Here as well.
                </body>
            </html>
        """.trimIndent()

        val index = "test-index-malicious"
        val testIndexService = createIndex(index)

        testIndexService.indexPageContents(
            SummitSearchIndexRequest(
            sourceUrl,
            Jsoup.parse(maliciousHtml)
        ))

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(source)
            },
            HtmlMapping::class.java
        )

        assertEquals("Hello World!", document?.source()?.seoDescription)
        assertEquals("There is some content here. Here too.", document?.source()?.paragraphContent)
        assertEquals("There is some content here. Here too. Here as well.", document?.source()?.rawTextContent)
    }

    @Test
    fun `excluded tags are removed before text is indexed`() {
        val source = "francisbaileyh.com/this-test"
        val sourceUrl = URL("http://$source")
        val htmlWithExcludedTags = """
            <html>
                <body>
                    <header><p>Here is header</p></header>
                    <nav><p>Some Nav Thing</p></nav>
                    <p>Test content.</p>
                    <ul>
                      <li><a>Bad Content</a></li>
                    </ul>
                    <footer><h1>Footer</h1></footer>
                </body>
            </html>
        """.trimIndent()

        val index = "test-index-excluded-tags"
        val testIndexService = createIndex(index)

        testIndexService.indexPageContents(
            SummitSearchIndexRequest(
                sourceUrl,
                Jsoup.parse(htmlWithExcludedTags)
            ))

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(source)
            },
            HtmlMapping::class.java
        )

        assertEquals("Test content.", document?.source()?.paragraphContent)
        assertEquals("Test content.", document?.source()?.rawTextContent)
    }

    @Test
    fun `pages without paragraphs or text are excluded`() {
        val index = "index-with-paragraph-free-text"
        val pages = setOf("NoParagraphs", "TwinsTower")

        val testIndexService = createIndex(index)

        pages.forEach {
            testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$sourceUrlString/$it"), htmlDocument = loadHtml(it)))
        }

        refreshIndex(index)

        val result = testIndexService.query(SummitSearchQueryRequest(
            term = "Twins Tower"
        ))

        assertEquals(1, result.hits.size)
        assertEquals("$sourceUrlString/TwinsTower", result.hits.first().source)
        assertEquals("Like Bill Corbett mentioned in this The 11,000ers book, \"the sudden view of <em>Twins</em> <em>Tower</em> from…", result.hits.first().highlight)
        assertEquals("Twins Tower | Steven's Peak-bagging Journey", result.hits.first().title)
    }

    @Test
    fun `excluded elements are removed from page on example site CascadeClimbers`() {
        val index = "index-with-excluded-elements"
        val pages = setOf("AssiniboineCC")

        val testIndexService = createIndex(index)

        pages.forEach {
            testIndexService.indexPageContents(SummitSearchIndexRequest(source = URL("$sourceUrlString/$it"), htmlDocument = loadHtml(it)))
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
        val url = URL("$sourceUrlString/LibertyBell")

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

    @Test
    fun `generate ID produces normalized ID from URL`() {
        val expectations = mapOf(
            "https://francisbailey.com" to "francisbailey.com",
            "http://francisbailey.com/path/to/" to "francisbailey.com/path/to",
            "http://francisbailey.com/path/to/?paramB=x&paramA=y" to "francisbailey.com/path/to?paramA=y&paramB=x",
            "https://francisbailey.com/path/to/index.html" to "francisbailey.com/path/to/index.html",
            "https://francisbailey.com/path/to/index.html?query=x&alpha=g" to "francisbailey.com/path/to/index.html?alpha=g&query=x",
            "http://subdomain.francisbailey.com" to "subdomain.francisbailey.com"
        )

        expectations.forEach {
            assertEquals(it.value, SummitSearchIndexService.generateId(URL(it.key)))
        }
    }

    @Test
    fun `query uses phrase + term when term count is greater than 2`() {
        val index = "phrase_term_test"
        val bestFieldTerm = "three terms here"
        val response = mock<SearchResponse<HtmlMapping>>()
        val hitsMetadata = mock<HitsMetadata<HtmlMapping>>()
        val testIndexService = SummitSearchIndexService(mockClient, 20, index)

        whenever(mockClient.search(any<SearchRequest>(), any<Class<HtmlMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(SummitSearchQueryRequest(bestFieldTerm))

        val expectedQuery = buildExpectedSearchQuery("\"three terms\" \"here\"", index)

       verify(mockClient).search(check<SearchRequest> { assertEquals(expectedQuery.toString(), it.toString()) }, any<Class<HtmlMapping>>())
    }

    @Test
    fun `query switches to phrase when term count is equal to or less than 2`() {
        val index = "phrase-match-test"
        val phraseFieldTerm = "two terms"
        val testIndexService = SummitSearchIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<HtmlMapping>>()
        val hitsMetadata = mock<HitsMetadata<HtmlMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<HtmlMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(SummitSearchQueryRequest(phraseFieldTerm))

        val expectedQuery = buildExpectedSearchQuery("\"two terms\"", index)

        verify(mockClient).search(check<SearchRequest> { assertEquals(it.toString(), expectedQuery.toString()) }, any<Class<HtmlMapping>>())
    }

    @Test
    fun `escapes non-alphanumeric characters from query`() {
        val index = "phrase-sanitization-test"
        val phraseFieldTerm = "(two*) terms!<>||( \"test\""
        val testIndexService = SummitSearchIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<HtmlMapping>>()
        val hitsMetadata = mock<HitsMetadata<HtmlMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<HtmlMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(SummitSearchQueryRequest(phraseFieldTerm))

        val expectedQuery = buildExpectedSearchQuery("\"two terms\" \"test\"", index)

        verify(mockClient).search(check<SearchRequest> { assertEquals(it.toString(), expectedQuery.toString()) }, any<Class<HtmlMapping>>())
    }

    private fun buildExpectedSearchQuery(term: String, index: String): SearchRequest {
        return SearchRequest.of {
            it.index(index)
            it.trackTotalHits { track ->
                track.enabled(true)
            }
            it.query { query ->
                query.simpleQueryString { match ->
                    match.query(term)
                    match.fields(
                        HtmlMapping::title.name.plus("^10"),
                        HtmlMapping::rawTextContent.name,
                        HtmlMapping::seoDescription.name.plus("^3"),
                        HtmlMapping::paragraphContent.name
                    )
                    match.minimumShouldMatch("100%")
                    match.analyzer(SummitSearchIndexService.ANALYZER_NAME)
                    match.defaultOperator(Operator.And)
                }
            }
            it.fields(listOf(
                FieldAndFormat.of { field ->
                    field.field(HtmlMapping::source.name)
                },
                FieldAndFormat.of { field ->
                    field.field(HtmlMapping::title.name)
                }
            ))
            it.source { sourceConfig ->
                sourceConfig.fetch(false)
            }
            it.highlight { highlight ->
                highlight.numberOfFragments(SummitSearchIndexService.HIGHLIGHT_FRAGMENT_COUNT)
                highlight.fragmentSize(SummitSearchIndexService.HIGHLIGHT_FRAGMENT_SIZE)
                highlight.fields(mapOf(
                    HtmlMapping::seoDescription.name to HighlightField.Builder().build(),
                    HtmlMapping::paragraphContent.name to HighlightField.Builder().build(),
                    HtmlMapping::rawTextContent.name to HighlightField.Builder().build()
                ))
                highlight.order(HighlighterOrder.Score)
            }
            it.size(20)
            it.from(0)
        }
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

    private fun indexHtmlContent(indexService: SummitSearchIndexService) {
        indexedPages.forEach {
            val sourceURL = URL("$sourceUrlString/$it")
            val html = loadHtml(it)
            indexService.indexPageContents(SummitSearchIndexRequest(sourceURL, html))
        }

        refreshIndex(indexService.indexName)
    }


}