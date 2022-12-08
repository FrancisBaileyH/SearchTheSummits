package com.francisbailey.summitsearch.frontend.controller


import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchQueryRequest
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController(
    private val summitSearchIndexService: SummitSearchIndexService
) {

    private val log = KotlinLogging.logger { }

    @GetMapping(SEARCH_API_PATH)
    fun search(@RequestParam(name = "query") requestQuery: String, @RequestParam(name = "next", required = false) next: Int?): ResponseEntity<SummitSearchResponse> {
        log.info { "Querying search service for: $requestQuery" }

        return try {
            val response = summitSearchIndexService.query(
                SummitSearchQueryRequest(
                    term = requestQuery,
                    from = next ?: 0
                )
            )

            ResponseEntity.ok(SummitSearchResponse(
                hits = response.hits.map {
                    SummitSearchHitResponse(
                        highlight = it.highlight,
                        source = it.source,
                        title = it.title
                    )
                },
                totalHits = response.totalHits,
                next = response.next
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    companion object {
        const val SEARCH_API_PATH = "/api/summits"
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