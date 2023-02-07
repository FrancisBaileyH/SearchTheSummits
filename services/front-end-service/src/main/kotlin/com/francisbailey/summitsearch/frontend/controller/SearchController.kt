package com.francisbailey.summitsearch.frontend.controller


import com.francisbailey.summitsearch.frontend.cdn.DigitalOceanCDNShim
import com.francisbailey.summitsearch.frontend.stats.QueryStatsReporter
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchQueryRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchQueryStat
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

@RestController
class SearchController(
    private val summitSearchIndexService: SummitSearchIndexService,
    private val queryStatsReporter: QueryStatsReporter,
    private val digitalOceanCdnShim: DigitalOceanCDNShim,
    private val meterRegistry: MeterRegistry
) {

    private val log = KotlinLogging.logger { }

    @GetMapping(path = [SEARCH_API_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(@RequestParam(name = "query") requestQuery: String, @RequestParam(name = "next", required = false) next: Int?): ResponseEntity<String> {
        log.info { "Querying search service for: $requestQuery and next value: $next" }

        return try {
            val response = meterRegistry.timer("api.searchindex.query.latency").recordCallable {
                summitSearchIndexService.query(
                    SummitSearchQueryRequest(
                        term = requestQuery,
                        from = next ?: 0
                    )
                )
            }!!

            if (response.totalHits == 0L) {
                meterRegistry.counter("api.searchindex.query.miss").increment()
            }

            queryStatsReporter.pushQueryStat(SummitSearchQueryStat(
                query = response.sanitizedQuery.lowercase(),
                page = next?.toLong(),
                totalHits = response.totalHits,
                timestamp = Instant.now().toEpochMilli()
            ))

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchHitResponse(
                        highlight = it.highlight,
                        source = it.source,
                        title = it.title.ifBlank { URL(it.source).host },
                        thumbnail = it.thumbnails?.firstOrNull()?.let { tUrl -> digitalOceanCdnShim.originToCDN(URL(tUrl)).toString() }
                    )
                }.groupBy { URL(it.source).host }.values,
                totalHits = response.totalHits,
                next = response.next
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
data class SummitSearchResponse(
    val hits: Collection<List<SummitSearchHitResponse>>,
    val totalHits: Long,
    val next: Int
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