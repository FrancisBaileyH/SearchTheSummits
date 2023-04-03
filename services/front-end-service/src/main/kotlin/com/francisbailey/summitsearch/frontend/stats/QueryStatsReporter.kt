package com.francisbailey.summitsearch.frontend.stats

import com.francisbailey.summitsearch.indexservice.QueryStatsIndex
import com.francisbailey.summitsearch.indexservice.QueryStat
import com.francisbailey.summitsearch.indexservice.QueryStatsPutRequest
import mu.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Best effort stats publisher. If the stats index is down for some reason, this
 * data will be lost until a retry mechanism is in place.
 */
@Service
class QueryStatsReporter(
    private val queryStatsService: QueryStatsIndex
) {
    private val log = KotlinLogging.logger { }

    private val stats = ArrayDeque<QueryStat>()

    fun pushQueryStat(stat: QueryStat) = synchronized(this) {
        stats.add(stat)
    }

    fun flushToIndex() {
        if (stats.isEmpty()) {
            return
        }

        log.info { "Flushing: ${stats.size.coerceAtMost(MAX_STATS_PER_PUSH)} stat(s) to index" }

        val statsToPush = generateSequence {
            stats.removeFirstOrNull()
        }

        try {
            queryStatsService.pushStats(
                QueryStatsPutRequest(
                    stats = statsToPush.take(MAX_STATS_PER_PUSH).toList()
                )
            )

            log.info { "Flushed stats successfully" }
        } catch (e: Exception) {
            log.error(e) { "Failed to flush stats" }
        }
    }

    companion object {
        const val MAX_STATS_PER_PUSH = 100
    }
}