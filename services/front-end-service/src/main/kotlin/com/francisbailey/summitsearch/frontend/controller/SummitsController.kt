package com.francisbailey.summitsearch.frontend.controller

import com.francisbailey.summitsearch.frontend.cdn.DigitalOceanCDNShim
import com.francisbailey.summitsearch.frontend.stats.QueryStatsReporter
import com.francisbailey.summitsearch.indexservice.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URL
import java.time.Instant
import kotlin.math.absoluteValue

@RestController
class SummitsController(
    private val summitSearchIndexService: SummitSearchIndexService,
    private val queryStatsReporter: QueryStatsReporter,
    private val digitalOceanCdnShim: DigitalOceanCDNShim,
    private val meterRegistry: MeterRegistry,
    private val documentResultsPerPage: Int,
) {
    private val log = KotlinLogging.logger { }

    @GetMapping(path = [SEARCH_API_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(
        @RequestParam(name = "query") requestQuery: String,
        @RequestParam(name = "page", required = false) page: Int?,
        @RequestParam(name = "sort", required = false) sort: String? = null,
        @RequestParam(name = "type", required = false) type: String? = null
    ): ResponseEntity<String> {
        log.info { "Querying search service for: $requestQuery and page: $page" }

        val sortType = when (sort?.lowercase()) {
            "date" -> SummitSearchSortType.BY_DATE
            else -> SummitSearchSortType.BY_RELEVANCE
        }

        val queryType = when(type?.lowercase()) {
            "fuzzy" -> SummitSearchQueryType.FUZZY
            else -> SummitSearchQueryType.STRICT
        }

        return try {
            val response = meterRegistry.timer("api.searchindex.query.latency").recordCallable {
                summitSearchIndexService.query(
                    SummitSearchQueryRequest(
                        term = requestQuery,
                        from = page?.absoluteValue?.dec()?.times(documentResultsPerPage) ?: 0,
                        sortType = sortType,
                        queryType = queryType
                    )
                )
            }!!

            queryStatsReporter.pushQueryStat(SummitSearchQueryStat(
                query = response.sanitizedQuery.lowercase(),
                page = page?.toLong(),
                totalHits = response.totalHits,
                timestamp = Instant.now().toEpochMilli(),
                type = queryType.name,
                sort = sortType.name,
                index = summitSearchIndexService.indexName
            ))

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchHitResponse(
                        highlight = it.highlight,
                        source = it.source,
                        title = it.title.ifBlank { URL(it.source).host },
                        thumbnail = it.thumbnails?.firstOrNull()?.let { tUrl -> digitalOceanCdnShim.originToCDN(URL(tUrl)).toString() }
                    )
                },
                totalHits = response.totalHits,
                next = response.next,
                resultsPerPage = documentResultsPerPage
            )))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(Json.encodeToString(SummitSearchErrorResponse(
                message = "Invalid argument: ${e.message}"
            )))
        }
    }

    companion object {
        const val SEARCH_API_PATH = "/api/summits"
    }

}

@Serializable
data class SummitSearchResponse<T>(
    val hits: List<T>,
    val totalHits: Long,
    val next: Int,
    val resultsPerPage: Int
)

@Serializable
data class SummitSearchHitResponse(
    val highlight: String,
    val source: String,
    val title: String,
    val thumbnail: String? = null
)

@Serializable
data class SummitSearchErrorResponse(
    val message: String
)