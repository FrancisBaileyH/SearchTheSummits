package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.frontend.stats.QueryStatsReporter
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit

@Configuration
open class BackgroundSchedulerConfiguration(
    private val queryStatsReporter: QueryStatsReporter
) {

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    open fun runReporter() {
        queryStatsReporter.flushToIndex()
    }

}