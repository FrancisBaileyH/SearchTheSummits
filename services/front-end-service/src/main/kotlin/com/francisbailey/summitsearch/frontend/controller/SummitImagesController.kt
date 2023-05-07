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
import org.springframework.web.util.HtmlUtils
import java.net.URL
import java.time.Instant
import javax.servlet.http.HttpServletRequest
import kotlin.math.absoluteValue

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
        @RequestParam(name = "page", required = false) page: Int?,
        @RequestParam(name = "sort", required = false) sort: String? = null,
        @RequestParam(name = "type", required = false) type: String? = null,
        request: HttpServletRequest
    ): ResponseEntity<String> {

        log.info { "Querying image service for: $requestQuery and next value: $page" }

        val sortType = when (sort?.lowercase()) {
            "date" -> DocumentSortType.BY_DATE
            else -> DocumentSortType.BY_RELEVANCE
        }

        val queryType = when(type?.lowercase()) {
            "fuzzy" -> DocumentQueryType.FUZZY
            else -> DocumentQueryType.STRICT
        }

        return try {
            val response = meterRegistry.timer("api.imageindex.query.latency").recordCallable {
                imageIndexService.query(ImageQueryRequest(
                    queryType = queryType,
                    sortType = sortType,
                    term = requestQuery,
                    from = page?.absoluteValue?.dec()?.times(IMAGE_RESULT_SIZE) ?: 0,
                ))
            }!!

            queryStatsReporter.pushQueryStat(QueryStat(
                query = response.sanitizedQuery.lowercase(),
                page = page?.toLong(),
                totalHits = response.totalHits,
                timestamp = Instant.now().toEpochMilli(),
                type = queryType.name,
                sort = sortType.name,
                index = imageIndexService.indexName,
                ipAddress = request.getHeader("x-forwarded-for")
            ))

            if (response.totalHits == 0L && queryType != DocumentQueryType.FUZZY) {
                return search(requestQuery, page, sort, "fuzzy", request)
            }

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    val description = it.description.ifBlank { it.referencingDocumentTitle } ?: ""

                    SummitSearchImageHitResponse(
                        description = HtmlUtils.htmlEscape(description),
                        source = it.source,
                        thumbnail = digitalOceanCdnShim.originToCDN(URL(it.dataStoreReference)).toString(),
                        referencingDocument = it.referencingDocument,
                        imageHeight = it.heightPx,
                        imageWidth = it.widthPx
                    )
                },
                totalHits = response.totalHits,
                next = response.next,
                resultsPerPage = IMAGE_RESULT_SIZE
            )))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(Json.encodeToString(SummitSearchErrorResponse(
                message = "Invalid argument: ${e.message}"
            )))
        }
    }

    @GetMapping(path = [SEARCH_PREVIEW_API_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun searchPreview(
        @RequestParam(name = "query") requestQuery: String,
        @RequestParam(name = "type", required = false) type: String? = null
    ): ResponseEntity<String> {

        val queryType = when(type?.lowercase()) {
            "fuzzy" -> DocumentQueryType.FUZZY
            else -> DocumentQueryType.STRICT
        }

        return try {
            val response = meterRegistry.timer("api.imageindex.query.latency").recordCallable {
                imageIndexService.query(ImageQueryRequest(
                    queryType = queryType,
                    sortType = DocumentSortType.BY_RELEVANCE,
                    term = requestQuery,
                    from = 0,
                    paginationResultSize = PREVIEW_IMAGE_RESULT_SIZE
                ))
            }!!

            if (response.totalHits == 0L && queryType != DocumentQueryType.FUZZY) {
                return searchPreview(requestQuery, "fuzzy")
            }

            ResponseEntity.ok(Json.encodeToString(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchImageHitResponse(
                        description = HtmlUtils.htmlEscape(it.description),
                        source = it.source,
                        thumbnail = digitalOceanCdnShim.originToCDN(URL(it.dataStoreReference)).toString(),
                        referencingDocument = it.referencingDocument,
                        imageHeight = it.heightPx,
                        imageWidth = it.widthPx
                    )
                },
                totalHits = response.totalHits,
                next = 0,
                resultsPerPage = PREVIEW_IMAGE_RESULT_SIZE
            )))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(Json.encodeToString(SummitSearchErrorResponse(
                message = "Invalid argument: ${e.message}"
            )))
        }
    }


    companion object {
        const val SEARCH_API_PATH = "/api/images"
        const val SEARCH_PREVIEW_API_PATH = "/api/images/preview"
        const val IMAGE_RESULT_SIZE = 30
        const val PREVIEW_IMAGE_RESULT_SIZE = 6
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