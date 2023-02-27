package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class QueryStatsIndexTest {

    @Test
    fun `creates index if it does not exist`() {
        val index = "stats-index-not-exists"
        val service = QueryStatsIndex(client, index)

        assertFalse(client.indexExists(index))

        service.createIfNotExists()

        assertTrue(client.indexExists(index))
    }

    @Test
    fun `pushes query stats on bulk into the index`() {
        val service = QueryStatsIndex(client).also {
            it.createIfNotExists()
        }

        val data = (0..4).map {
            SummitSearchQueryStat(
                query = "Some Test $it",
                page = it.toLong(),
                timestamp = Instant.now().toEpochMilli(),
                totalHits = 1,
                type = "test",
                sort = "date"
            )
        }

        service.pushStats(SummitSearchQueryStatsPutRequest(data))

        client.indices().refresh(RefreshRequest.of {
            it.index(QueryStatsIndex.INDEX_NAME)
        })

        val result = client.search(SearchRequest.of {
            it.index(QueryStatsIndex.INDEX_NAME)
            it.query { query ->
                query.match { matchQuery ->
                    matchQuery.query("Some Test")
                    matchQuery.field(QueryStat::query.name)
                }
            }
        }, QueryStat::class.java)

        val queries = result.hits().hits().map { it.source()!!.query }.toSet()
        assertEquals(data.map { it.query }.toSet(), queries)
    }

    companion object {
        private val testServer = ElasticSearchTestServer.global()

        private val client = testServer.client()
    }
}