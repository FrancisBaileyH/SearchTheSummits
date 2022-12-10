package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.frontend.controller.SearchController
import com.francisbailey.summitsearch.frontend.throttling.ThrottlingService
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import java.time.Duration

@Configuration
open class ThrottlingConfiguration(
    private val rateLimiterFactory: RateLimiterFactory
) {

    val apiRequestPerSecondMap = hashMapOf(
        ThrottlingService.rateLimiterKey(HttpMethod.GET, SearchController.SEARCH_API_PATH) to 5
    )
}

@Service
class RateLimiterFactory {
    fun build(requestsPerSecond: Int): RateLimiterRegistry {
        return RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(requestsPerSecond)
                .timeoutDuration(Duration.ofMillis(0))
                .build()
        )
    }
}