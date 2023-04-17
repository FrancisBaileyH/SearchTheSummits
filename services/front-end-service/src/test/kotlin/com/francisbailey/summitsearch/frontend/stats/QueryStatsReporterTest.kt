package com.francisbailey.summitsearch.frontend.stats

import com.francisbailey.summitsearch.indexservice.QueryStatsIndex
import com.francisbailey.summitsearch.indexservice.QueryStat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import java.time.Instant

class QueryStatsReporterTest {

    private val queryStatsIndex = mock<QueryStatsIndex>()

    private val reporter = QueryStatsReporter(queryStatsIndex)

    @Test
    fun `adds stats and the pushes them to index`() {
        val stat = QueryStat(
            query = "test",
            timestamp = Instant.now().toEpochMilli(),
            totalHits = 1,
            page = 1,
            type = "test",
            sort = "test",
            index = "test",
            ipAddress = "127.0.0.1"
        )
        reporter.pushQueryStat(stat)
        reporter.flushToIndex()

        verify(queryStatsIndex).pushStats(org.mockito.kotlin.check {
            assertEquals(1, it.stats.size)
            assertEquals(stat.query, it.stats.first().query)
        })

        reporter.flushToIndex()
        verifyNoMoreInteractions(queryStatsIndex)
    }


    @Test
    fun `pushes at most 100 stats per flush index call`() {
        val stats = (0..200).map {
            QueryStat(
                query = "test $it",
                timestamp = Instant.now().toEpochMilli(),
                totalHits = 1,
                page = 1,
                type = "test",
                sort = "test",
                index = "test",
                ipAddress = "127.0.0.1"
            )
        }

        stats.forEach {
            reporter.pushQueryStat(it)
        }
        reporter.flushToIndex()

        verify(queryStatsIndex).pushStats(org.mockito.kotlin.check {
            assertEquals(100, it.stats.size)
        })
    }


}