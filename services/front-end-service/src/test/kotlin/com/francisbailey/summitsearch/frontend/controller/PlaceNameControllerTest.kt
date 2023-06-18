package com.francisbailey.summitsearch.frontend.controller

import com.francisbailey.summitsearch.indexservice.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PlaceNameControllerTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val placeNameIndexService = mock<PlaceNameIndexService>()

    private val controller = PlaceNamesController(meterRegistry, placeNameIndexService)

    @Test
    fun `search returns a query form placename index`() {
        whenever(placeNameIndexService.query(any())).thenReturn(listOf(
            PlaceNameHit(
                name = "Test Case Peak",
                alternativeName = "Mount Test Suite",
                description = "This is a test",
                elevation = 1500,
                source = "This test suite",
                latitude = 12.0,
                longitude = 14.1
            )
        ))

        val response = controller.search("Test Case Peak")
        val expectedResponse = PlaceNameSearchResponse(listOf(
            PlaceNameSearchHit(
                name = "Test Case Peak",
                alternativeName = "Mount Test Suite",
                description = "This is a test",
                elevation = 1500,
                source = "This test suite",
                latitude = 12.0,
                longitude = 14.1
            )
        ))

        verify(placeNameIndexService).query(PlaceNameQueryRequest("Test Case Peak"))
        assertEquals(Json.encodeToString(expectedResponse), response.body)
    }

    @Test
    fun `search returns an autocomplete query form placename index`() {
        whenever(placeNameIndexService.autoCompleteQuery(any())).thenReturn(listOf(
            PlaceNameSuggestion(
                displayName = "Test Case Peak",
                suggestion = "Test Case Peak"
            )
        ))

        val response = controller.autoCompleteSearch("Test Case Peak")
        val expectedResponse = PlaceNameSearchResponse(listOf(
            PlaceNameAutoCompleteHit(
                displayName = "Test Case Peak",
                suggestion = "Test Case Peak"
            )
        ))

        verify(placeNameIndexService).autoCompleteQuery(AutoCompleteQueryRequest("Test Case Peak"))
        assertEquals(Json.encodeToString(expectedResponse), response.body)
    }
}