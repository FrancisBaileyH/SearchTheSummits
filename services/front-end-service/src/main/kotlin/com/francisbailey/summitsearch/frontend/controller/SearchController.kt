package com.francisbailey.summitsearch.frontend.controller


import com.francisbailey.summitsearch.frontend.FaviconServicePrototype
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchQueryRequest
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

@RestController
class SearchController(
    private val summitSearchIndexService: SummitSearchIndexService,
    private val meterRegistry: MeterRegistry,
    private val faviconServicePrototype: FaviconServicePrototype
) {

    private val log = KotlinLogging.logger { }

    @GetMapping(path = [SEARCH_API_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(@RequestParam(name = "query") requestQuery: String, @RequestParam(name = "next", required = false) next: Int?): ResponseEntity<String> {
        log.info { "Querying search service for: $requestQuery" }

        return try {
            val response = meterRegistry.timer("api.searchindex.query.latency").recordCallable {
                summitSearchIndexService.query(
                    SummitSearchQueryRequest(
                        term = requestQuery,
                        from = next ?: 0
                    )
                )
            }!!

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchHitResponse(
                        highlight = it.highlight,
                        source = it.source,
                        title = it.title,
                        favicon = faviconServicePrototype.getFavicon(URL(it.source)).imageData
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
    val favicon: String
)

@Serializable
data class SummitSearchErrorResponse(
    val message: String
)