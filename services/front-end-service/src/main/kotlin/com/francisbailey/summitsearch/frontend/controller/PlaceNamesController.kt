package com.francisbailey.summitsearch.frontend.controller

import com.francisbailey.summitsearch.indexservice.AutoCompleteQueryRequest
import com.francisbailey.summitsearch.indexservice.PlaceNameIndexService
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

@RestController
class PlaceNamesController(
    private val meterRegistry: MeterRegistry,
    private val placeNameIndexService: PlaceNameIndexService
) {

    private val log = KotlinLogging.logger { }

    @GetMapping(path = [AUTO_COMPLETE_PLACENAME_SEARCH_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun autoCompleteSearch(
        @RequestParam(name = "query") requestQuery: String,
    ): ResponseEntity<String> {
        log.info { "Auto complete query for: $requestQuery" }

        return try {
            val response = meterRegistry.timer("api.placenameindex.auto-complete-query.latency").recordCallable {
                placeNameIndexService.autoCompleteQuery(AutoCompleteQueryRequest(prefix = requestQuery))
            }!!

            ResponseEntity.ok().body(Json.encodeToString(PlaceNameAutoCompleteResponse(
                response.map {
                    PlaceNameAutoCompleteHit(
                        displayName = it.displayName,
                        suggestion = it.suggestion
                    )
                }
            )))
        } catch (e: Exception) {
            log.error(e) { "Failed to process query: $requestQuery" }
            ResponseEntity.badRequest().body(
                Json.encodeToString(SummitSearchErrorResponse(
                message = "Bad Request"
            )))
        }
    }

    companion object {
        const val AUTO_COMPLETE_PLACENAME_SEARCH_PATH = "/api/placenames/autocomplete"
        const val PLACENAME_SEARCH_PATH = "/api/placenames"
    }
}

@Serializable
data class PlaceNameAutoCompleteHit(
    val displayName: String,
    val suggestion: String
)

@Serializable
data class PlaceNameAutoCompleteResponse(
    val placenames: List<PlaceNameAutoCompleteHit>
)