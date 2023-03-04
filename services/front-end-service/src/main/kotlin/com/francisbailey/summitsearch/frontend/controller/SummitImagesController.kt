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

@RestController
class SummitImagesController(
    private val imageIndexService: ImageIndexService,
    private val queryStatsReporter: QueryStatsReporter,
    private val digitalOceanCdnShim: DigitalOceanCDNShim,
    private val meterRegistry: MeterRegistry
) {

    private val log = KotlinLogging.logger { }

    @GetMapping(path = [SEARCH_API_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(
        @RequestParam(name = "query") requestQuery: String,
        @RequestParam(name = "next", required = false) next: Int?,
        @RequestParam(name = "sort", required = false) sort: String? = null,
        @RequestParam(name = "type", required = false) type: String? = null
    ): ResponseEntity<String> {

        log.info { "Querying image service for: $requestQuery and next value: $next" }

        val sortType = when (sort?.lowercase()) {
            "date" -> SummitSearchSortType.BY_DATE
            else -> SummitSearchSortType.BY_RELEVANCE
        }

        val queryType = when(type?.lowercase()) {
            "fuzzy" -> SummitSearchQueryType.FUZZY
            else -> SummitSearchQueryType.STRICT
        }

        return try {
            val response = meterRegistry.timer("api.imageindex.query.latency").recordCallable {
                imageIndexService.query(SummitSearchImagesQueryRequest(
                    queryType = queryType,
                    sortType = sortType,
                    term = requestQuery,
                    from = next ?: 0
                ))
            }!!

            queryStatsReporter.pushQueryStat(SummitSearchQueryStat(
                query = response.sanitizedQuery.lowercase(),
                page = next?.toLong(),
                totalHits = response.totalHits,
                timestamp = Instant.now().toEpochMilli(),
                type = queryType.name,
                sort = sortType.name,
                index = imageIndexService.indexName
            ))

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchImageHitResponse(
                        description = it.description,
                        source = it.source,
                        thumbnail = digitalOceanCdnShim.originToCDN(URL(it.dataStoreReference)).toString(),
                        referencingDocument = it.referencingDocument,
                        imageHeight = it.heightPx,
                        imageWidth = it.widthPx
                    )
                },
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
        const val SEARCH_API_PATH = "/api/images"
    }
}

@Serializable
data class SummitSearchImageHitResponse(
    val source: String,
    val description: String,
    val thumbnail: String,
    val referencingDocument: String,
    val imageHeight: Int,
    val imageWidth: Int
)