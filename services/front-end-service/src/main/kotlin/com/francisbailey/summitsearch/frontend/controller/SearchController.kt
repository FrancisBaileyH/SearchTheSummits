package com.francisbailey.summitsearch.frontend.controller


import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchQueryRequest
import mu.KotlinLogging
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(
    private val summitSearchIndexService: SummitSearchIndexService
) {

    private val log = KotlinLogging.logger { }

    /**
     * @TODO rate limiting, request validation/sanitization
     * @TODO certain summits only
     */
    @GetMapping("/api/summits")
    fun search(@RequestParam(name = "query") requestQuery: String, @RequestParam(name = "next", required = false) next: Int?): SummitSearchResponse {
        log.info { "Querying search service for: $requestQuery" }

        val response = try {
            summitSearchIndexService.query(
                SummitSearchQueryRequest(
                    term = requestQuery,
                    from = next ?: 0
                )
            )
        } catch (e: IllegalArgumentException) {
            throw ServletRequestBindingException("Invalid parameters for: $requestQuery or $next")
        }

        return SummitSearchResponse(
            hits = response.hits.map {
                SummitSearchHitResponse(
                    highlight = it.highlight,
                    source = it.source,
                    title = it.title
                )
            },
            totalHits = response.totalHits,
            next = response.next
        )
    }

}

data class SummitSearchResponse(
    val hits: List<SummitSearchHitResponse>,
    val totalHits: Long,
    val next: Int
)

data class SummitSearchHitResponse(
    val highlight: String,
    val source: String,
    val title: String
)