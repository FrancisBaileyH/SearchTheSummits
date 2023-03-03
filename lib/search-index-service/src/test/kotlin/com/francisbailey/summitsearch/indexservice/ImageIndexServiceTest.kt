package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.GetResponse
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.elasticsearch.core.search.HitsMetadata
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration
import java.time.Instant

class ImageIndexServiceTest {

    private val mockClient = mock<ElasticsearchClient>()

    private val getDocument: (URL, String) -> GetResponse<ImageMapping> = { url, index ->
        client.get(GetRequest.of {
            it.index(index)
            it.id(generateIdFromUrl(url))
        }, ImageMapping::class.java)
    }

    @Test
    fun `indexes image with expected content`() {
        val request = SummitSearchImagePutRequest(
            source = URL("https://francisbaileyh.com/wp-content/image.jpeg?w=1230"),
            referencingDocumentDate = 123456789,
            referencingDocument = URL("https://francisbaileyh.com/some-page"),
            description = "This is a mountain!",
            dataStoreReference = "https://some-reference-here",
            normalizedSource = URL("https://francisbaileyh.com/wp-content/image.jpeg")
        )

        val index = "create-image-index"
        val testIndexService = ImageIndexService(client, 10, index).also {
            it.createIfNotExists()
        }

        assertFalse(getDocument(request.source, index).found())

        testIndexService.indexImage(request)

        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = getDocument(request.normalizedSource, index)

        assertEquals(request.description, result.source()!!.description)
        assertEquals(request.dataStoreReference, result.source()!!.dataStoreReference)
        assertEquals(request.referencingDocumentDate, result.source()!!.referencingDocumentDate)
        assertEquals(request.referencingDocument.host, result.source()!!.referencingDocumentHost)
        assertEquals(request.source.toString(), result.source()!!.source)
        assertEquals(generateIdFromUrl(request.normalizedSource), result.id())
    }

    @Test
    fun `cleans malicious text from description`() {
        val request = SummitSearchImagePutRequest(
            source = URL("https://francisbaileyh.com/wp-content/image.jpeg"),
            referencingDocumentDate = 123456789,
            referencingDocument = URL("https://francisbaileyh.com/some-page"),
            description = "This is a mountain!<script>alert(something);</script><p>Hello?</p>",
            dataStoreReference = "https://some-reference-here",
            normalizedSource = URL("https://francisbaileyh.com/wp-content/image.jpeg")
        )

        val index = "create-image-index-bad-text"
        val testIndexService = ImageIndexService(client, 10, index).also {
            it.createIfNotExists()
        }

        assertFalse(getDocument(request.source, index).found())

        testIndexService.indexImage(request)

        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = getDocument(request.source, index)

        assertEquals("This is a mountain!Hello?", result.source()!!.description)
    }


    @Test
    fun `creates index with html analyzer if index does not exist yet`() {
        val index = "create-image-index-not-exist"
        val testIndexService = ImageIndexService(client, 10, index)
        assertFalse(client.indexExists(index))

        testIndexService.createIfNotExists()

        assertTrue(client.indexExists(index))
    }

    @Test
    fun `can query results using fuzzy search`() {
        val index = "image-fuzzy-query-test"
        val testIndexService = ImageIndexService(client, 20, index).also {
            it.createIfNotExists()
        }

        val searchTerm = "Mount Last"

        val indexRequest = SummitSearchImagePutRequest(
            source = URL("https://www.francisbaileyh.com/test.png?123"),
            description = "Mount Lastt",
            referencingDocument = URL("https://www.francisbaileyh.com/high-score"),
            dataStoreReference = "some-reference",
            referencingDocumentDate = Instant.now().toEpochMilli(),
            normalizedSource = URL("https://www.francisbaileyh.com/test.png")
        )

        testIndexService.indexImage(indexRequest)
        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = testIndexService.query(SummitSearchImagesQueryRequest(term = searchTerm, queryType = SummitSearchQueryType.FUZZY))

        assertEquals(1, result.hits.size)
        assertEquals(indexRequest.source.toString(), result.hits.first().source)
        assertEquals(indexRequest.description, result.hits.first().description)
    }

    @Test
    fun `can query results using strict search`() {
        val index = "image-strict-query-test"
        val testIndexService = ImageIndexService(client, 20, index).also {
            it.createIfNotExists()
        }
        val searchTerm = "Mount Last"

        val indexRequest = SummitSearchImagePutRequest(
            source = URL("https://www.francisbaileyh.com/test.png?123"),
            description = "This mount is called $searchTerm",
            referencingDocument = URL("https://www.francisbaileyh.com/high-score"),
            dataStoreReference = "some-reference",
            referencingDocumentDate = Instant.now().toEpochMilli(),
            normalizedSource = URL("https://www.francisbaileyh.com/test.png")
        )

        testIndexService.indexImage(indexRequest)
        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = testIndexService.query(SummitSearchImagesQueryRequest(term = searchTerm, queryType = SummitSearchQueryType.STRICT))

        assertEquals(1, result.hits.size)
        assertEquals(indexRequest.source.toString(), result.hits.first().source)
        assertEquals(indexRequest.description, result.hits.first().description)
    }

    @Test
    fun `sorts by date when request specifies sortByDate type`() {
        val index = "image-sort-by-date"
        val testIndexService = ImageIndexService(client, 20, index).also {
            it.createIfNotExists()
        }
        val instant = Instant.now()
        val searchTerm = "Mount Last"

        val normalScoreRequests = (0..2L).map {
            SummitSearchImagePutRequest(
                source = URL("https://www.francisbaileyh.com/$it"),
                description = searchTerm,
                referencingDocument = URL("https://www.francisbaileyh.com/high-score"),
                dataStoreReference = "some-reference",
                referencingDocumentDate = instant.plus(Duration.ofDays(it)).toEpochMilli(),
                normalizedSource = URL("https://www.francisbaileyh.com/high-score/test$it.png")
            )
        }

        normalScoreRequests.forEach {
            testIndexService.indexImage(it)
        }

        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val sortByDateQueryResult = testIndexService.query(SummitSearchImagesQueryRequest(term = searchTerm, sortType = SummitSearchSortType.BY_DATE))

        normalScoreRequests.reversed().forEachIndexed { i, it ->
            assertEquals(it.source.toString(), sortByDateQueryResult.hits[i].source)
        }
        assertEquals("https://www.francisbaileyh.com/2", sortByDateQueryResult.hits.first().source)
        assertEquals("https://www.francisbaileyh.com/0", sortByDateQueryResult.hits.last().source)
        assertEquals(3, sortByDateQueryResult.hits.size)
    }

    @Test
    fun `performs simple query string query when exact match specified in request`() {
        val index = "image-exact-match-test"
        val phraseFieldTerm = "this is a query"
        val testIndexService = ImageIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<ImageMapping>>()
        val hitsMetadata = mock<HitsMetadata<ImageMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<ImageMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(SummitSearchImagesQueryRequest(phraseFieldTerm, queryType = SummitSearchQueryType.STRICT))

        verify(mockClient).search(org.mockito.kotlin.check<SearchRequest> {
            assertEquals(20, it.size())
            assertEquals(""""this is" "a" "query"""", it.query()?.simpleQueryString()?.query())
        }, any<Class<ImageMapping>>())
    }

    @Test
    fun `performs fuzzy query when exact match specified in request`() {
        val index = "image-fuzzy-match-test"
        val phraseFieldTerm = "this is a query"
        val testIndexService = ImageIndexService(mockClient, 20, index)

        val response = mock<SearchResponse<ImageMapping>>()
        val hitsMetadata = mock<HitsMetadata<ImageMapping>>()

        whenever(mockClient.search(any<SearchRequest>(), any<Class<ImageMapping>>())).thenReturn(response)
        whenever(response.hits()).thenReturn(hitsMetadata)
        whenever(hitsMetadata.hits()).thenReturn(emptyList())

        testIndexService.query(SummitSearchImagesQueryRequest(phraseFieldTerm, queryType = SummitSearchQueryType.FUZZY))

        verify(mockClient).search(org.mockito.kotlin.check<SearchRequest> {
            assertEquals(20, it.size())
            assertEquals("""this is a query""", it.query()?.match()?.query()?.stringValue())
            assertEquals(Operator.And, it.query()?.match()?.operator())
        }, any<Class<ImageMapping>>())
    }
    companion object {
        private val testServer = ElasticSearchTestServer.global()

        private val client = testServer.client()
    }

}