package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch.core.ExistsRequest
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.elasticsearch.core.search.HighlightField
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder
import co.elastic.clients.elasticsearch.core.search.HitsMetadata
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import com.francisbailey.summitsearch.indexservice.test.ElasticSearchTestServer
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
import java.time.LocalDateTime
import java.time.ZoneOffset

class DocumentIndexServiceTest {

    private val resources = File("src/test/resources")

    private val indexedPages = setOf("LibertyBell", "LilyRock", "TexasPeak", "ContentWithoutParagraphs")

    private val source = "www.francisbaileyh.com"

    private val sourceUrlString = "https://$source"

    private val mockClient = mock<ElasticsearchClient>()

    @Test
    fun `returns false if index does not exist and true when it does`() {
        val index = "non-existent-index"
        assertFalse(client.indexExists(index))

        client.indices().create(CreateIndexRequest.of {
            it.index(index)
        })

        assertTrue(client.indexExists(index))
    }

    @Test
    fun `creates index with html analyzer if index does not exist yet`() {
        val index = "create-index-not-exist"
        val testIndexService = DocumentIndexService(client, 10, index)
        assertFalse(client.indexExists(index))

        testIndexService.createIfNotExists()

        assertTrue(client.indexExists(index))
    }

    @Test
    fun `query throws IllegalArgumentException when from value exceeds maximum`() {
        val testIndexService = createIndex("test")

        assertThrows<IllegalArgumentException> {
            testIndexService.query(DocumentQueryRequest(term = "test", from = DocumentIndexService.MAX_FROM_VALUE + 1))
        }
    }

    @Test
    fun `query throws IllegalArgumentException when max term size exceeded`() {
        val testIndexService = createIndex("test")

        val term = StringBuffer().apply {
            repeat(DocumentIndexService.MAX_QUERY_TERM_SIZE + 1) {
                this.append("a")
            }
        }.toString()

        assertThrows<IllegalArgumentException> {
            testIndexService.query(DocumentQueryRequest(term = term, from = 1))
        }
    }

    @Test
    fun `query term lily returns exactly one result with expected source and search highlight`() {
        val index = "test-index-lily"
        val testIndexService = createIndex(index)
        indexHtmlContent(testIndexService)

        val results = testIndexService.query(DocumentQueryRequest(term = "Lily"))

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

        val results = testIndexService.query(DocumentQueryRequest(term = "Texas Peak"))

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

        val results = testIndexService.query(DocumentQueryRequest(term = "Connaught"))

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

        val paginatedResults = generateSequence(testIndexService.query(DocumentQueryRequest(term = "Peak"))) {
            if (it.hits.isNotEmpty()) {
                testIndexService.query(DocumentQueryRequest(term = "Peak", from = it.next))
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

        assertTrue(testIndexService.query(DocumentQueryRequest(term = "Liberty")).hits.isEmpty())
        testIndexService.indexContent(HtmlDocumentPutRequest(source = URL("$sourceUrlString/LibertyBell"), htmlDocument = page))
        refreshIndex(index)

        val result = testIndexService.query(DocumentQueryRequest(term = "Liberty"))

        assertFalse(result.hits.isEmpty())

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id("$source/LibertyBell")
            },
            DocumentMapping::class.java
        )
        assertTrue(document.found())
        assertEquals(URL(sourceUrlString).host, document.source()?.host)
        assertEquals("In fact, there's a very high concentration of", document.source()?.seoDescription)
    }

    @Test
    fun `page is indexed with partition id on bulk index page request`() {
        val index = "bulk-partition-page-test"
        val testIndexService = createIndex(index)
        val url = URL("https://francisbaileyh.com/some-page")

        testIndexService.indexPartitionedContent(listOf(
            DocumentPutRequest(
                source = url,
                rawTextContent = "Test",
                paragraphContent = "",
                seoDescription = "",
                title = "Hello World"
            )
        ))
        refreshIndex(index)

        val exists = client.exists(ExistsRequest.of {
            it.index(index)
            it.id("francisbaileyh.com/some-page-0")
        })

        assertTrue(exists.value())
    }

    @Test
    fun `pageCreationDate is set if date value is not null`() {
        val index = "test-index-liberty-with-date"
        val testIndexService = createIndex(index)

        val date = LocalDateTime.now()
        val page = loadHtml("LibertyBell")

        assertTrue(testIndexService.query(DocumentQueryRequest(term = "Liberty")).hits.isEmpty())
        testIndexService.indexContent(HtmlDocumentPutRequest(
            source = URL("$sourceUrlString/LibertyBell"),
            htmlDocument = page,
            pageCreationDate = date
        ))
        refreshIndex(index)

        val result = testIndexService.query(DocumentQueryRequest(term = "Liberty"))

        assertFalse(result.hits.isEmpty())

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id("$source/LibertyBell")
            },
            DocumentMapping::class.java
        )
        assertTrue(document.found())
        assertEquals(URL(sourceUrlString).host, document.source()?.host)
        assertEquals(date.toInstant(ZoneOffset.UTC).toEpochMilli(), document.source()?.pageCreationDate!!)
    }

    @Test
    fun `title fallsback to URL if title value is missing`() {
        val index = "missing-title-index"
        val testIndexService = createIndex(index)
        val sourceURL = URL("http://francisbaileyh.com/test")

        val html = """
           <html>
             <body>
               <p>Some Test</p>
             </body>
           </html>
        """

        testIndexService.indexContent(
            HtmlDocumentPutRequest(
            source = sourceURL,
            htmlDocument = Jsoup.parse(html)
        ))

        refreshIndex(index)

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(generateIdFromUrl(sourceURL))
            },
            DocumentMapping::class.java
        )

        assertEquals(sourceURL.host, document.source()?.title)
    }

    @Test
    fun `malicious text stripped from content fields on indexing html page`() {
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

        val index = "test-index-malicious-html"
        val testIndexService = createIndex(index)

        testIndexService.indexContent(
            HtmlDocumentPutRequest(
            sourceUrl,
            Jsoup.parse(maliciousHtml)
        ))

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(source)
            },
            DocumentMapping::class.java
        )

        assertEquals("Hello World!", document?.source()?.seoDescription)
        assertEquals("There is some content here. Here too.", document?.source()?.paragraphContent)
        assertEquals("There is some content here. Here too. Here as well.", document?.source()?.rawTextContent)
    }

    @Test
    fun `malicious text stripped from content fields on indexing`() {
        val source = "francisbaileyh.com/this-test"
        val sourceUrl = URL("http://$source")
        val index = "test-index-malicious-text"
        val testIndexService = createIndex(index)

        testIndexService.indexContent(
            DocumentIndexRequest(
                source = sourceUrl,
                rawTextContent = "<p>There is some content here.</p> <p>Here <script>alert('test');</script>too.</p>",
                seoDescription = "<script>alert('test')/</script>Hello <center>World!</center>",
                paragraphContent = "Test <script>Something</script><p>hello</p>",
                title = "<title><script>alert('test')/</script>Hello <center>World!</center></title>"
            ))

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(source)
            },
            DocumentMapping::class.java
        )

        assertEquals("Hello World!", document?.source()?.seoDescription)
        assertEquals("Test hello", document?.source()?.paragraphContent)
        assertEquals("There is some content here. Here too.", document?.source()?.rawTextContent)
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

        testIndexService.indexContent(
            HtmlDocumentPutRequest(
                sourceUrl,
                Jsoup.parse(htmlWithExcludedTags)
            ))

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(source)
            },
            DocumentMapping::class.java
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
            testIndexService.indexContent(HtmlDocumentPutRequest(source = URL("$sourceUrlString/$it"), htmlDocument = loadHtml(it)))
        }

        refreshIndex(index)

        val result = testIndexService.query(DocumentQueryRequest(
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
            testIndexService.indexContent(HtmlDocumentPutRequest(source = URL("$sourceUrlString/$it"), htmlDocument = loadHtml(it)))
        }

        refreshIndex(index)

        val result = testIndexService.query(DocumentQueryRequest(
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

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        assertEquals(url.toString(), testIndexService.query(DocumentQueryRequest(term = "Liberty")).hits.first().source)

        testIndexService.deletePageContents(DocumentDeleteRequest(url))
        refreshIndex(index)

        val result = client.get(GetRequest.of {
            it.index(index)
            it.id(generateIdFromUrl(url))
        }, DocumentMapping::class.java)

        assertFalse(result.found())
    }

    @Test
    fun `query uses phrase + term when term count is greater than 2`() {
        val index = "phrase_term_test"
        val bestFieldTerm = "three terms here"
        val response = mock<SearchResponse<DocumentMapping>>()
        val hitsMetadata = mock<HitsMetadata<DocumentMapping>>()
        val testIndexService = DocumentIndexService(mockClient, 20, index)

        whenever(mockClient.search(any<SearchRequest>(), any<Class<DocumentMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(DocumentQueryRequest(bestFieldTerm))

        val expectedQuery = buildExpectedSearchQuery("\"three terms\" \"here\"", index)

       verify(mockClient).search(check<SearchRequest> { assertEquals(expectedQuery.toString(), it.toString()) }, any<Class<DocumentMapping>>())
    }

    @Test
    fun `query switches to phrase when term count is equal to or less than 2`() {
        val index = "phrase-match-test"
        val phraseFieldTerm = "two terms"
        val testIndexService = DocumentIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<DocumentMapping>>()
        val hitsMetadata = mock<HitsMetadata<DocumentMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<DocumentMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(DocumentQueryRequest(phraseFieldTerm))

        val expectedQuery = buildExpectedSearchQuery("\"two terms\"", index)

        verify(mockClient).search(check<SearchRequest> { assertEquals(it.toString(), expectedQuery.toString()) }, any<Class<DocumentMapping>>())
    }

    @Test
    fun `escapes non-alphanumeric characters from query`() {
        val index = "phrase-sanitization-test"
        val phraseFieldTerm = "Term-with-hyphen *produces| this (query) ~here and tom's 0123456789’!"
        val testIndexService = DocumentIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<DocumentMapping>>()
        val hitsMetadata = mock<HitsMetadata<DocumentMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<DocumentMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(DocumentQueryRequest(phraseFieldTerm))

        val expectedQuery = buildExpectedSearchQuery(""""Term\-with\-hyphen produces" "this" "query" "here" "and" "tom's" "0123456789’"""", index)

        verify(mockClient).search(check<SearchRequest> { assertEquals(it.toString(), expectedQuery.toString()) }, any<Class<DocumentMapping>>())
    }

    @Test
    fun `pushes documents on bulk into the index`() {
        val index = "bulk-test"
        val testIndexService = createIndex(index)

        val data = (0..4).map {
            DocumentIndexRequest(
                source = URL("https://abc.com"),
                rawTextContent = "TEST$it",
                title = "Some title$it",
                seoDescription = "",
                paragraphContent = ""
            )
        }

        testIndexService.indexPartitionedContent(data)

        refreshIndex(index)

        val result = client.search(SearchRequest.of {
            it.index(index)
            it.query { query ->
                query.matchAll(MatchAllQuery.Builder().build())
            }
        }, DocumentMapping::class.java)

        val sources = result.hits().hits().map { it.source()!!.source }.toSet()
        assertEquals(data.map { it.source}.toSet(), sources)
    }

    @Test
    fun `throws exception if bulk index size is greater than max`() {
        val service = DocumentIndexService(mockClient, 10, "test")

        val requests = (0..service.maxBulkIndexRequests).map {
            mock<DocumentIndexRequest> {
                on(mock.source).thenReturn(URL("https://www.example.com"))
            }
        }

        assertThrows<IllegalArgumentException> { service.indexPartitionedContent(requests) }
    }

    @Test
    fun `throws exception if document source differs on any bulk request`() {
        val service = DocumentIndexService(mockClient, 10, "test")

        val request1 = mock<DocumentIndexRequest>()
        val request2 = mock<DocumentIndexRequest>()

        whenever(request1.source).thenReturn(URL("https://example.com"))
        whenever(request2.source).thenReturn(URL("https://some-other-site.com"))

        assertThrows<IllegalArgumentException> {
            service.indexPartitionedContent(listOf(
                request1,
                request2
            ))
        }
    }

    @Test
    fun `adds thumbnails to document on update call`() {
        val index = "thumbnail-index-test"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")
        val url = URL("$sourceUrlString/LibertyBell")

        val thumnbails = listOf("data-reference-1", "data-reference-2")

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(generateIdFromUrl(url))
            },
            DocumentMapping::class.java
        )

        assertNotNull(document)
        assertNull(document?.source()?.thumbnails)

        testIndexService.putThumbnails(
            DocumentThumbnailPutRequest(
            source = url,
            dataStoreReferences = thumnbails
        )
        )
        refreshIndex(index)

        val documentWithThumbnails = client.get(
            GetRequest.of {
                it.index(index)
                it.id(generateIdFromUrl(url))
            },
            DocumentMapping::class.java
        )
        assertEquals(thumnbails, documentWithThumbnails?.source()?.thumbnails)
    }

    @Test
    fun `adds thumbnails to document and is available in query after`() {
        val index = "thumbnail-index-test-query"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")
        val url = URL("$sourceUrlString/LibertyBell")

        val thumnbails = listOf("data-reference-1", "data-reference-2")

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        val document = client.get(
            GetRequest.of {
                it.index(index)
                it.id(generateIdFromUrl(url))
            },
            DocumentMapping::class.java
        )

        assertNotNull(document)
        assertNull(document?.source()?.thumbnails)

        testIndexService.putThumbnails(
            DocumentThumbnailPutRequest(
                source = url,
                dataStoreReferences = thumnbails
            )
        )
        refreshIndex(index)

        val results = testIndexService.query(DocumentQueryRequest(term = "Liberty"))

        assertEquals(thumnbails, results.hits.first().thumbnails)
    }

    @Test
    fun `index call does not override thumbnail field`() {
        val index = "thumbnail-index-override"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")
        val url = URL("$sourceUrlString/LibertyBell")

        val thumnbails = listOf("data-reference-1", "data-reference-2")

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        testIndexService.putThumbnails(
            DocumentThumbnailPutRequest(
                source = url,
                dataStoreReferences = thumnbails
            )
        )
        refreshIndex(index)

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        val results = testIndexService.query(DocumentQueryRequest(term = "Liberty"))
        assertEquals(thumnbails, results.hits.first().thumbnails)
    }

    @Test
    fun `exists call returns true if exists and false otherwise`() {
        val index = "test-index-exists-test"
        val testIndexService = createIndex(index)

        val page = loadHtml("LibertyBell")
        val url = URL("$sourceUrlString/LibertyBell")


        assertFalse(testIndexService.pageExists(DocumentExistsRequest(url)))

        testIndexService.indexContent(HtmlDocumentPutRequest(source = url, htmlDocument = page))
        refreshIndex(index)

        assertTrue(testIndexService.pageExists(DocumentExistsRequest(url)))
    }

    @Test
    fun `sorts by date when request specifies sortByDate type`() {
        val index = "sort-by-date-test"
        val testIndexService = createIndex(index)
        val dateTime = LocalDateTime.now()
        val searchTerm = "Mount Last"

        val highScoreRequest = DocumentIndexRequest(
            source = URL("https://www.francisbaileyh.com/high-score"),
            title = searchTerm,
            rawTextContent = "This mountain is called: $searchTerm",
            paragraphContent = "This mountain is called: $searchTerm",
            seoDescription = "This mountain is called: $searchTerm",
            pageCreationDate = dateTime
        )

        val normalScoreRequests = (0..2).map {
            DocumentIndexRequest(
                source = URL("https://www.francisbaileyh.com/$it"),
                title = "A standard title $it",
                rawTextContent = "This mountain is called: $searchTerm",
                paragraphContent = "",
                seoDescription = "",
                pageCreationDate = dateTime.plusDays(it.toLong())
            )
        }

        normalScoreRequests.plus(highScoreRequest).forEach {
            testIndexService.indexContent(it)
        }

        refreshIndex(index)

        val standardQueryResult = testIndexService.query(DocumentQueryRequest(term = searchTerm))

        assertEquals("https://www.francisbaileyh.com/high-score", standardQueryResult.hits.first().source)
        assertEquals(4, standardQueryResult.hits.size)


        val sortByDateQueryResult = testIndexService.query(DocumentQueryRequest(term = searchTerm, sortType = DocumentSortType.BY_DATE))

        normalScoreRequests.reversed().forEachIndexed { requestIndex, it ->
            assertEquals(it.source.toString(), sortByDateQueryResult.hits[requestIndex].source)
        }
        assertEquals("https://www.francisbaileyh.com/high-score", sortByDateQueryResult.hits.last().source)
        assertEquals(4, sortByDateQueryResult.hits.size)
    }

    @Test
    fun `performs fuzzy query when specified in request`() {
        val index = "fuzzy-match-test"
        val phraseFieldTerm = "this is a query"
        val testIndexService = DocumentIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<DocumentMapping>>()
        val hitsMetadata = mock<HitsMetadata<DocumentMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<DocumentMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(DocumentQueryRequest(phraseFieldTerm, queryType = DocumentQueryType.FUZZY))

        val expectedQuery = buildExpectedFuzzyQuery("this is a query", index)

        verify(mockClient).search(check<SearchRequest> { assertEquals(it.toString(), expectedQuery.toString()) }, any<Class<DocumentMapping>>())
    }

    private fun buildExpectedFuzzyQuery(term: String, index: String): SearchRequest {
        return SearchRequest.of {
            it.index(index)
            it.trackTotalHits { track ->
                track.enabled(true)
            }
            it.query { query ->
                query.multiMatch { match ->
                    match.query(term)
                    match.fields(
                        DocumentMapping::title.name.plus("^10"), // boost title the highest
                        DocumentMapping::rawTextContent.name,
                        DocumentMapping::seoDescription.name.plus("^3"), // boost the SEO description score
                        DocumentMapping::paragraphContent.name
                    )
                    match.analyzer(DocumentIndexService.ANALYZER_NAME)
                    match.operator(Operator.And)
                    match.minimumShouldMatch("100%")
                    match.fuzziness("AUTO")
                    match.maxExpansions(2)
                }
            }
            it.fields(listOf(
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::source.name)
                },
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::title.name)
                },
                FieldAndFormat.of {field ->
                    field.field(DocumentMapping::thumbnails.name)
                }
            ))
            it.source { sourceConfig ->
                sourceConfig.fetch(false)
            }
            it.highlight { highlight ->
                highlight.numberOfFragments(DocumentIndexService.HIGHLIGHT_FRAGMENT_COUNT)
                highlight.fragmentSize(DocumentIndexService.HIGHLIGHT_FRAGMENT_SIZE)
                highlight.fields(mapOf(
                    DocumentMapping::seoDescription.name to HighlightField.Builder().build(),
                    DocumentMapping::paragraphContent.name to HighlightField.Builder().build(),
                    DocumentMapping::rawTextContent.name to HighlightField.Builder().build()
                ))
                highlight.order(HighlighterOrder.Score)
                highlight.noMatchSize(DocumentIndexService.HIGHLIGHT_FRAGMENT_SIZE)
            }
            it.size(20)
            it.from(0)
        }
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
                        DocumentMapping::title.name.plus("^10"),
                        DocumentMapping::rawTextContent.name,
                        DocumentMapping::seoDescription.name.plus("^3"),
                        DocumentMapping::paragraphContent.name
                    )
                    match.minimumShouldMatch("100%")
                    match.analyzer(DocumentIndexService.ANALYZER_NAME)
                    match.defaultOperator(Operator.And)
                }
            }
            it.fields(listOf(
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::source.name)
                },
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::title.name)
                },
                FieldAndFormat.of {field ->
                    field.field(DocumentMapping::thumbnails.name)
                }
            ))
            it.source { sourceConfig ->
                sourceConfig.fetch(false)
            }
            it.highlight { highlight ->
                highlight.numberOfFragments(DocumentIndexService.HIGHLIGHT_FRAGMENT_COUNT)
                highlight.fragmentSize(DocumentIndexService.HIGHLIGHT_FRAGMENT_SIZE)
                highlight.fields(mapOf(
                    DocumentMapping::seoDescription.name to HighlightField.Builder().build(),
                    DocumentMapping::paragraphContent.name to HighlightField.Builder().build(),
                    DocumentMapping::rawTextContent.name to HighlightField.Builder().build()
                ))
                highlight.order(HighlighterOrder.Score)
                highlight.noMatchSize(DocumentIndexService.HIGHLIGHT_FRAGMENT_SIZE)
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

    private fun createIndex(index: String, pagination: Int = 10): DocumentIndexService {
        return DocumentIndexService(client, pagination, index).also {
            it.createIfNotExists()
        }
    }

    private fun loadHtml(name: String): Document {
        return Jsoup.parse(resources.resolve("$name.html").absoluteFile.readText())
    }

    private fun indexHtmlContent(indexService: DocumentIndexService) {
        indexedPages.forEach {
            val sourceURL = URL("$sourceUrlString/$it")
            val html = loadHtml(it)
            indexService.indexContent(HtmlDocumentPutRequest(sourceURL, html))
        }

        refreshIndex(indexService.indexName)
    }

    companion object {
        private val testServer = ElasticSearchTestServer.global()

        private val client = testServer.client()
    }


}