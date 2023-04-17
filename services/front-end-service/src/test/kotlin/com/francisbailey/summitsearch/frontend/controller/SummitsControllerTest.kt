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

class SummitsControllerTest {

    private val documentIndexService = mock<DocumentIndexService> {
        on(mock.indexName).thenReturn("summit-index")
    }
    private val queryStatsReporter = mock<QueryStatsReporter>()
    private val digitalOceanCdnShim = mock<DigitalOceanCDNShim>()
    private val meterRegistry = SimpleMeterRegistry()

    private val controller = SummitsController(
        documentIndexService = documentIndexService,
        queryStatsReporter = queryStatsReporter,
        digitalOceanCdnShim = digitalOceanCdnShim,
        meterRegistry = meterRegistry
    )

    private val request = mock<HttpServletRequest> {
        on(mock.getHeader(any())).thenReturn("")
    }

    @Test
    fun `returns bad response when illegal argument exception occurs`() {
        whenever(documentIndexService.query(any())).thenThrow(IllegalArgumentException("Test"))

        val response = controller.search("Test", null, request = request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `returns expected response and pushes to query stats reporter`() {
        val result = PaginatedDocumentResult(
            hits = listOf(
                DocumentQueryHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(documentIndexService.query(any())).thenReturn(result)

        val response = controller.search("some test", null, request = request)

        val expectedResponse = SummitSearchResponse(
            hits = listOf(
                SummitSearchHitResponse(
                    highlight = result.hits.first().highlight,
                    source = result.hits.first().source,
                    title = result.hits.first().title,
                    thumbnail = null
                )
            ),
            totalHits = result.totalHits,
            next = 0,
            resultsPerPage = 10
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
        verify(queryStatsReporter).pushQueryStat(org.mockito.kotlin.check {
            assertEquals(result.sanitizedQuery, it.query)
            assertEquals(result.totalHits, it.totalHits)
        })
        verify(documentIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_RELEVANCE, it.sortType)
            assertEquals(DocumentQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `returns shimmed cdn url if thumbnail is present`() {
        val shimmedThumbnailUrl = URL("http://shimmed.com")
        val result = PaginatedDocumentResult(
            hits = listOf(
                DocumentQueryHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = listOf("https://123.com")
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(documentIndexService.query(any())).thenReturn(result)
        whenever(digitalOceanCdnShim.originToCDN(any())).thenReturn(shimmedThumbnailUrl)

        val response = controller.search("some test", null, request = request)

        val expectedResponse = SummitSearchResponse(
            hits = listOf(
                SummitSearchHitResponse(
                    highlight = result.hits.first().highlight,
                    source = result.hits.first().source,
                    title = result.hits.first().title,
                    thumbnail = shimmedThumbnailUrl.toString()
                )
            ),
            totalHits = result.totalHits,
            next = 0,
            resultsPerPage = 10
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
        verify(queryStatsReporter).pushQueryStat(org.mockito.kotlin.check {
            assertEquals(result.sanitizedQuery, it.query)
            assertEquals(result.totalHits, it.totalHits)
        })
        verify(documentIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_RELEVANCE, it.sortType)
            assertEquals(DocumentQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `sorts by date when date sort parameter is passed in`() {
        val result = PaginatedDocumentResult(
            hits = listOf(
                DocumentQueryHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(documentIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, "date", request = request)

        verify(documentIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_DATE, it.sortType)
            assertEquals(DocumentQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `uses fuzzy match when fuzzy parameter is set`() {
        val result = PaginatedDocumentResult(
            hits = listOf(
                DocumentQueryHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(documentIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, "date", "fuzzy", request = request)

        verify(documentIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(DocumentSortType.BY_DATE, it.sortType)
            assertEquals(DocumentQueryType.FUZZY, it.queryType)
        })
    }
}