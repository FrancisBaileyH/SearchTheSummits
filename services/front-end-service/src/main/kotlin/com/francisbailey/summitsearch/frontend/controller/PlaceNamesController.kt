package com.francisbailey.summitsearch.frontend.controller

import com.francisbailey.summitsearch.indexservice.AutoCompleteQueryRequest
import com.francisbailey.summitsearch.indexservice.PlaceNameIndexService
import com.francisbailey.summitsearch.indexservice.PlaceNameQueryRequest
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

    @GetMapping(path = [PLACENAME_SEARCH_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun search(
        @RequestParam(name = "query") requestQuery: String
    ): ResponseEntity<String> {
        log.info { "Placename query for: $requestQuery" }

        return try {
            val response = meterRegistry.timer("api.placenameindex.query.latency").recordCallable {
                placeNameIndexService.query(PlaceNameQueryRequest(query = requestQuery))
            }!!

            ResponseEntity.ok().body(Json.encodeToString(PlaceNameSearchResponse(
                response.map {
                    PlaceNameSearchHit(
                        name = it.name,
                        alternativeName = it.alternativeName,
                        elevation = it.elevation,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        description = it.description,
                        source = it.source
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

    @GetMapping(path = [AUTO_COMPLETE_PLACENAME_SEARCH_PATH], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun autoCompleteSearch(
        @RequestParam(name = "query") requestQuery: String,
    ): ResponseEntity<String> {
        log.info { "Auto complete query for: $requestQuery" }

        return try {
            val response = meterRegistry.timer("api.placenameindex.auto-complete-query.latency").recordCallable {
                placeNameIndexService.autoCompleteQuery(AutoCompleteQueryRequest(prefix = requestQuery))
            }!!

            ResponseEntity.ok().body(Json.encodeToString(PlaceNameSearchResponse(
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
data class PlaceNameSearchResponse<T>(
    val placenames: List<T>
)

@Serializable
data class PlaceNameSearchHit(
    val name: String,
    val alternativeName: String? = null,
    val elevation: Int?,
    val description: String? = null,
    val source: String? = null,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class PlaceNameAutoCompleteHit(
    val displayName: String,
    val suggestion: String
)