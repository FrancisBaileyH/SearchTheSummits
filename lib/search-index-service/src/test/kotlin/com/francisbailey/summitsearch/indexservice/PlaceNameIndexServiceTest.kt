package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery
import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import com.francisbailey.summitsearch.indexservice.common.toSha1
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import com.francisbailey.summitsearch.indexservice.test.ElasticSearchTestServer
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceNameIndexServiceTest {

    @Test
    fun `creates index if it does not exist`() {
        val index = "placename-index-not-exists"
        val service = PlaceNameIndexService(index, client)

        Assertions.assertFalse(client.indexExists(index))
        service.createIfNotExists()
        Assertions.assertTrue(client.indexExists(index))
    }

    @Test
    fun `indexes content with expected ID and content`() {
        val service = PlaceNameIndexService(client = client, indexName = "placename-document-test").also {
            it.createIfNotExists()
        }

        val request = PlaceNameIndexRequest(
            name = "Mount Hanover",
            alternativeName = "Something Made Up",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788,
            description = "This is a mountain",
            source = "NRCAN"
        )

        service.index(request)
        client.indices().refresh()

        val result = client.get(GetRequest.of {
            it.index(service.indexName)
            it.id("${request.name.lowercase()}-${request.latitude}-${request.longitude}".toSha1())
        }, PlaceNameMapping::class.java).source()

        assertEquals(request.name, result?.name)
        assertEquals(request.alternativeName, result?.alternativeName)
        assertEquals(request.latitude, result?.location?.lat)
        assertEquals(request.longitude, result?.location?.lon)
        assertEquals(request.elevation, result?.elevation)
        assertEquals(request.description, result?.description)
        assertEquals(request.source, result?.source)
    }

    @Test
    fun `indexes bulk content with expected ID and content`() {
        val service = PlaceNameIndexService(client = client, indexName = "placename-document-test").also {
            it.createIfNotExists()
        }

        val request = PlaceNameIndexRequest(
            name = "Mount Hanover",
            alternativeName = "Something Made Up",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788,
            description = "This is a mountain",
            source = "NRCAN"
        )

        val request2 = PlaceNameIndexRequest(
            name = "Mount Harvey",
            alternativeName = "Something Made Up",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788,
            description = "This is a mountain",
            source = "NRCAN"
        )

        service.index(listOf(request, request2))
        client.indices().refresh()

        val result = client.get(GetRequest.of {
            it.index(service.indexName)
            it.id("${request.name.lowercase()}-${request.latitude}-${request.longitude}".toSha1())
        }, PlaceNameMapping::class.java).source()

        assertEquals(request.name, result?.name)
        assertEquals(request.alternativeName, result?.alternativeName)
        assertEquals(request.latitude, result?.location?.lat)
        assertEquals(request.longitude, result?.location?.lon)
        assertEquals(request.elevation, result?.elevation)
        assertEquals(request.description, result?.description)
        assertEquals(request.source, result?.source)

        val result2 = client.get(GetRequest.of {
            it.index(service.indexName)
            it.id("${request2.name.lowercase()}-${request2.latitude}-${request2.longitude}".toSha1())
        }, PlaceNameMapping::class.java).source()

        assertEquals(request2.name, result2?.name)
        assertEquals(request2.alternativeName, result2?.alternativeName)
        assertEquals(request2.latitude, result2?.location?.lat)
        assertEquals(request2.longitude, result2?.location?.lon)
        assertEquals(request2.elevation, result2?.elevation)
        assertEquals(request2.description, result2?.description)
        assertEquals(request2.source, result2?.source)
    }

    @Test
    fun `indexes content with unique ID`() {
        val service = PlaceNameIndexService(client = client, indexName = "uid-test").also {
            it.createIfNotExists()
        }

        val requestA = PlaceNameIndexRequest(
            name = "Goat Mountain",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788,
            description = "This is a mountain",
            source = "NRCAN"
        )

        val requestB = PlaceNameIndexRequest(
            name = "Goat Mountain",
            latitude = 49.497778,
            longitude = -123.201389,
            elevation = 1788,
            description = "This is a mountain",
            source = "NRCAN"
        )

        service.index(requestA)
        service.index(requestB)
        client.indices().refresh()

        val results = client.search(SearchRequest.of {
            it.index(service.indexName)
            it.query { query ->
                query.matchAll(MatchAllQuery.Builder().build())
            }
        }, PlaceNameMapping::class.java)

        assertEquals(2L, results.hits().total()?.value())
    }

    @Test
    fun `indexed document is auto-complete searchable after`() {
        val service = PlaceNameIndexService(client = client, indexName = "basic-autocomplete").also {
            it.createIfNotExists()
        }

        val request = PlaceNameIndexRequest(
            name = "Mount Hanover",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788
        )

        service.index(request)
        client.indices().refresh()

        val results = service.autoCompleteQuery(AutoCompleteQueryRequest(prefix = "mount"))

        assertEquals(request.name, results.first().suggestion)
    }

    @Test
    fun `auto-complete searches alternative names as well`() {
        val service = PlaceNameIndexService(client = client, indexName = "basic-autocomplete-alt").also {
            it.createIfNotExists()
        }

        val request = PlaceNameIndexRequest(
            name = "Mount Hanover",
            alternativeName = "Mount Madeup",
            latitude = 49.487778,
            longitude = -123.201389,
            elevation = 1788
        )

        service.index(request)
        client.indices().refresh()

        val results = service.autoCompleteQuery(AutoCompleteQueryRequest(prefix = "mount m"))
        assertEquals(request.alternativeName, results.first().suggestion)
    }

    @Test
    fun `autocomplete only suggests documents with same prefix`() {
        val places = listOf("Mount Hanover", "Mount Harvey", "Hope Mountain")

        val service = PlaceNameIndexService(client = client, indexName = "multi-autocomplete").also {
            it.createIfNotExists()
        }

        val requests = places.mapIndexed { index, name ->
            PlaceNameIndexRequest(
                name = name,
                latitude = 49.48777 + index,
                longitude = -123.201389 - index,
                elevation = 1788 + index
            )
        }

        requests.forEach {
            service.index(it)
        }

        client.indices().refresh()

        val results = service.autoCompleteQuery(AutoCompleteQueryRequest(prefix = "mount"))
        val expectedSuggestions = requests
            .filter { it.name.startsWith("Mount") }
            .map { it.name }

        assertEquals(2, results.size)
        assertEquals(expectedSuggestions, results.map { it.suggestion })
    }

    @Test
    fun `autocomplete refines result to one if only one matching prefix`() {
        val places = listOf("Mount Hanover", "Mount Harvey", "Hope Mountain")

        val service = PlaceNameIndexService(client = client, indexName = "multi-autocomplete").also {
            it.createIfNotExists()
        }

        val requests = places.mapIndexed { index, name ->
            PlaceNameIndexRequest(
                name = name,
                latitude = 49.48777 + index,
                longitude = -123.201389 - index,
                elevation = 1788 + index
            )
        }

        requests.forEach {
            service.index(it)
        }

        client.indices().refresh()

        val results = service.autoCompleteQuery(AutoCompleteQueryRequest(prefix = "mount han"))

        assertEquals(1, results.size)
        assertEquals("Mount Hanover", results.first().suggestion)
    }

    @Test
    fun `indexed place names are fetched with match prefix`() {
        val places = listOf("Mount Hanover", "Mount Harvey", "Hope Mountain")

        val service = PlaceNameIndexService(client = client, indexName = "match-prefix-test").also {
            it.createIfNotExists()
        }

        val requests = places.mapIndexed { index, name ->
            PlaceNameIndexRequest(
                name = name,
                latitude = 49.48777 + index,
                longitude = -123.201389 - index,
                elevation = 1788 + index
            )
        }

        requests.forEach {
            service.index(it)
        }

        client.indices().refresh()

        val results = service.query(PlaceNameQueryRequest(query = "mount h"))
        val expectedSuggestions = requests
            .filter { it.name.startsWith("Mount") }
            .map { it.name }

        assertEquals(2, results.size)
        assertEquals(expectedSuggestions, results.map { it.name })
    }

    @Test
    fun `indexed place names are fetched with match prefix using alternative name`() {
        val places = listOf("Mount Hanover", "Mount Harvey", "Hope Mountain")

        val service = PlaceNameIndexService(client = client, indexName = "match-prefix-alt-name-test").also {
            it.createIfNotExists()
        }

        val requests = places.mapIndexed { index, name ->
            PlaceNameIndexRequest(
                name = name,
                alternativeName = "Mount Madeup$index",
                latitude = 49.48777 + index,
                longitude = -123.201389 - index,
                elevation = 1788 + index
            )
        }

        requests.forEach {
            service.index(it)
        }

        client.indices().refresh()

        val results = service.query(PlaceNameQueryRequest(query = "mount ma"))

        assertEquals(3, results.size)
        assertEquals(requests.map { it.name }, results.map { it.name })
    }

    companion object {
        private val testServer = ElasticSearchTestServer.global()
        private val client = testServer.client()
    }

}