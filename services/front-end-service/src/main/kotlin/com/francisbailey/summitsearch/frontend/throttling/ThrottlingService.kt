package com.francisbailey.summitsearch.frontend.throttling

import com.francisbailey.summitsearch.frontend.configuration.ThrottlingConfiguration
import com.francisbailey.summitsearch.services.common.RateLimiter
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
class ThrottlingService(
    private val rateLimiterFactory: RateLimiterFactory,
    private val defaultApiRateLimiter: RateLimiter<String>,
    throttlingConfiguration: ThrottlingConfiguration
) {
    private val apiThrottleMap = hashMapOf<String, RateLimiter<String>>()

    init {
        throttlingConfiguration.apiRequestPerSecondMap.forEach {
            apiThrottleMap[it.key] = rateLimiterFactory.build(it.value)
        }
    }

    fun shouldThrottle(request: HttpServletRequest): Boolean {
        val throttleKey = request.getHeader("x-forwarded-for") ?: defaultThrottleKey
        val rateLimiterKey = rateLimiterKey(HttpMethod.valueOf(request.method), request.servletPath)

        val rateLimiter = apiThrottleMap[rateLimiterKey] ?: defaultApiRateLimiter

        return !rateLimiter.tryConsume(throttleKey)
    }

    companion object {
        fun rateLimiterKey(method: HttpMethod, path: String): String {
            return "$method-$path"
        }

        const val defaultThrottleKey = "*"
    }

}