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

class SummitsControllerTest {

    private val summitSearchIndexService = mock<SummitSearchIndexService> {
        on(mock.indexName).thenReturn("summit-index")
    }
    private val queryStatsReporter = mock<QueryStatsReporter>()
    private val digitalOceanCdnShim = mock<DigitalOceanCDNShim>()
    private val meterRegistry = SimpleMeterRegistry()

    private val controller = SummitsController(
        summitSearchIndexService = summitSearchIndexService,
        queryStatsReporter = queryStatsReporter,
        digitalOceanCdnShim = digitalOceanCdnShim,
        meterRegistry = meterRegistry
    )


    @Test
    fun `returns bad response when illegal argument exception occurs`() {
        whenever(summitSearchIndexService.query(any())).thenThrow(IllegalArgumentException("Test"))

        val response = controller.search("Test", null)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `returns expected response and pushes to query stats reporter`() {
        val result = SummitSearchPaginatedResult(
            hits = listOf(
                SummitSearchHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(summitSearchIndexService.query(any())).thenReturn(result)

        val response = controller.search("some test", null)

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
            next = 0
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
        verify(queryStatsReporter).pushQueryStat(org.mockito.kotlin.check {
            assertEquals(result.sanitizedQuery, it.query)
            assertEquals(result.totalHits, it.totalHits)
        })
        verify(summitSearchIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(SummitSearchSortType.BY_RELEVANCE, it.sortType)
            assertEquals(SummitSearchQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `returns shimmed cdn url if thumbnail is present`() {
        val shimmedThumbnailUrl = URL("http://shimmed.com")
        val result = SummitSearchPaginatedResult(
            hits = listOf(
                SummitSearchHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = listOf("https://123.com")
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(summitSearchIndexService.query(any())).thenReturn(result)
        whenever(digitalOceanCdnShim.originToCDN(any())).thenReturn(shimmedThumbnailUrl)

        val response = controller.search("some test", null)

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
            next = 0
        )

        assertEquals(Json.encodeToString(expectedResponse), response.body)
        verify(queryStatsReporter).pushQueryStat(org.mockito.kotlin.check {
            assertEquals(result.sanitizedQuery, it.query)
            assertEquals(result.totalHits, it.totalHits)
        })
        verify(summitSearchIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(SummitSearchSortType.BY_RELEVANCE, it.sortType)
            assertEquals(SummitSearchQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `sorts by date when date sort parameter is passed in`() {
        val result = SummitSearchPaginatedResult(
            hits = listOf(
                SummitSearchHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(summitSearchIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, "date")

        verify(summitSearchIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(SummitSearchSortType.BY_DATE, it.sortType)
            assertEquals(SummitSearchQueryType.STRICT, it.queryType)
        })
    }

    @Test
    fun `uses fuzzy match when fuzzy parameter is set`() {
        val result = SummitSearchPaginatedResult(
            hits = listOf(
                SummitSearchHit(
                    highlight = "test",
                    title = "test",
                    source = "https://somewhere.com",
                    thumbnails = emptyList()
                )
            ),
            totalHits = 1,
            sanitizedQuery = "some test"
        )

        whenever(summitSearchIndexService.query(any())).thenReturn(result)

        controller.search("some test", null, "date", "fuzzy")

        verify(summitSearchIndexService).query(org.mockito.kotlin.check {
            assertEquals("some test", it.term)
            assertEquals(SummitSearchSortType.BY_DATE, it.sortType)
            assertEquals(SummitSearchQueryType.FUZZY, it.queryType)
        })
    }
}