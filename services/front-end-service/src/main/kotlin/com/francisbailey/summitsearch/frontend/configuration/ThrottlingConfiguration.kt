package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.frontend.controller.PlaceNamesController
import com.francisbailey.summitsearch.frontend.controller.SummitImagesController
import com.francisbailey.summitsearch.frontend.controller.SummitsController
import com.francisbailey.summitsearch.frontend.throttling.ThrottlingService
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import java.time.Duration

@Configuration
open class ThrottlingConfiguration {

    val apiRequestPerSecondMap = hashMapOf(
        ThrottlingService.rateLimiterKey(HttpMethod.GET, SummitsController.SEARCH_API_PATH) to 3,
        ThrottlingService.rateLimiterKey(HttpMethod.GET, SummitImagesController.SEARCH_API_PATH) to 3,
        ThrottlingService.rateLimiterKey(HttpMethod.GET, SummitImagesController.SEARCH_PREVIEW_API_PATH) to 3,
        ThrottlingService.rateLimiterKey(HttpMethod.GET, PlaceNamesController.PLACENAME_SEARCH_PATH) to 1,
        ThrottlingService.rateLimiterKey(HttpMethod.GET, PlaceNamesController.AUTO_COMPLETE_PLACENAME_SEARCH_PATH) to 4
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