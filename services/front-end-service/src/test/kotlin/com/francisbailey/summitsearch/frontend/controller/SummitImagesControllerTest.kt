package com.francisbailey.summitsearch.frontend.controller

import com.francisbailey.summitsearch.frontend.cdn.DigitalOceanCDNShim
import com.francisbailey.summitsearch.frontend.stats.QueryStatsReporter
import com.francisbailey.summitsearch.indexservice.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import java.net.URL
import javax.servlet.http.HttpServletRequest

class SummitImagesControllerTest {

    private val queryStatsReporter = mock<QueryStatsReporter>()
    private val digitalOceanCdnShim = mock<DigitalOceanCDNShim>()
    private val meterRegistry = SimpleMeterRegistry()
    private val imageIndexService = mock<ImageIndexService> {
        on(mock.indexName).thenReturn("image-index")
    }

    private val request = mock<HttpServletRequest> {
        on(mock.getHeader(any())).thenReturn("")
    }

    private val controller = SummitImagesController(
        queryStatsReporter = queryStatsReporter,
        digitalOceanCdnShim = digitalOceanCdnShim,
        meterRegistry = meterRegistry,
        imageIndexService = imageIndexService
    )

    @Test
    fun `api returns bad response when illegal argument exception occurs`() {
        whenever(imageIndexService.query(any())).thenThrow(IllegalArgumentException("Test"))

        val response = controller.search("Test", null, request = request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `images api returns expected response with shimmed thumbnail url`() {
        val shimmedThumbnailUrl = URL("http://shimmed.com")
        val result = PaginatedDocumentResult(
            hits = listOf(
                Image(
                    description = "test",
                    referencingDocument = "test",
                    source = "https://somewhere.com",
                    dataStoreReference = "https://123.com",
                    heightPx = 120,
                    widthPx = 200,
                    referencingDocumentTitle = "Test"
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)
        whenever(digitalOceanCdnShim.originToCDN(any())).thenReturn(shimmedThumbnailUrl)

        val response = controller.search("some test", null, request = request)

        val expectedResponse = SummitSearchResponse(
            hits = listOf(
                SummitSearchImageHitResponse(
                    description = result.hits.first().description,
                    referencingDocument = result.hits.first().referencingDocument,
                    source = result.hits.first().source,
                    thumbnail = shimmedThumbnailUrl.toString(),
                    imageHeight = result.hits.first().heightPx,
                    imageWidth = result.hits.first().widthPx
                )
            ),
            totalHits = result.totalHits,
            next = 0,
            resultsPerPage = 30
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
    }

    @Test
    fun `images api pushes to query stats reporter`() {
        val result = PaginatedDocumentResult<Image>(
            hits = emptyList(),
            totalHits = 1,
            sanitizedQuery = "some query"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)

        controller.search("some query", page = 0, request = request)

        verify(queryStatsReporter).pushQueryStat(org.mockito.kotlin.check {
            assertEquals(result.sanitizedQuery, it.query)
            assertEquals(result.totalHits, it.totalHits)
            assertEquals(imageIndexService.indexName, it.index)
        })
    }

    @Test
    fun `images api uses exact match and relevance sort by default`() {
        val result = PaginatedDocumentResult(
            hits = emptyList<Image>(),
            totalHits = 0,
            sanitizedQuery = "some test"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, null, null, request = request)

        verify(imageIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_RELEVANCE, it.sortType)
            assertEquals(DocumentQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `images api uses fuzzy match when fuzzy parameter is set`() {
        val result = PaginatedDocumentResult(
            hits = emptyList<Image>(),
            totalHits = 0,
            sanitizedQuery = "some test"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, "date", "fuzzy", request = request)

        verify(imageIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_DATE, it.sortType)
            assertEquals(DocumentQueryType.FUZZY, it.queryType)
        })
    }

    @Test
    fun `images preview api returns expected response with shimmed thumbnail url`() {
        val shimmedThumbnailUrl = URL("http://shimmed.com")
        val result = PaginatedDocumentResult(
            hits = listOf(
                Image(
                    description = "test",
                    referencingDocument = "test",
                    source = "https://somewhere.com",
                    dataStoreReference = "https://123.com",
                    heightPx = 120,
                    widthPx = 200,
                    referencingDocumentTitle = "Test"
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)
        whenever(digitalOceanCdnShim.originToCDN(any())).thenReturn(shimmedThumbnailUrl)

        val response = controller.searchPreview("some test", null)

        val expectedResponse = SummitSearchResponse(
            hits = listOf(
                SummitSearchImageHitResponse(
                    description = result.hits.first().description,
                    referencingDocument = result.hits.first().referencingDocument,
                    source = result.hits.first().source,
                    thumbnail = shimmedThumbnailUrl.toString(),
                    imageHeight = result.hits.first().heightPx,
                    imageWidth = result.hits.first().widthPx
                )
            ),
            totalHits = result.totalHits,
            next = 0,
            resultsPerPage = 6
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
    }

    @Test
    fun `images preview api uses fuzzy match when fuzzy parameter is set`() {
        val result = PaginatedDocumentResult(
            hits = emptyList<Image>(),
            totalHits = 0,
            sanitizedQuery = "some test"
        )

        whenever(imageIndexService.query(any())).thenReturn(result)

        controller.searchPreview("some test", "fuzzy")

        verify(imageIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentQueryType.FUZZY, it.queryType)
        })
    }
}