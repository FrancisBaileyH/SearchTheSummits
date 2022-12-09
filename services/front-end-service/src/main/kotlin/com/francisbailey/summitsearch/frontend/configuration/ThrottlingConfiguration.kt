package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.frontend.controller.SearchController
import com.francisbailey.summitsearch.frontend.throttling.RateLimiterFactory
import com.francisbailey.summitsearch.frontend.throttling.ThrottlingService
import com.francisbailey.summitsearch.services.common.RateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod

@Configuration
open class ThrottlingConfiguration(
    private val rateLimiterFactory: RateLimiterFactory
) {

    val apiRequestPerSecondMap = hashMapOf(
        ThrottlingService.rateLimiterKey(HttpMethod.GET, SearchController.SEARCH_API_PATH) to 5L
    )

    @Bean
    open fun defaultApiRateLimiter(): RateLimiter<String> {
        return rateLimiterFactory.build(50L)
    }
}