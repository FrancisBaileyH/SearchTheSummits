package com.francisbailey.summitsearch.frontend.throttling

import com.francisbailey.summitsearch.frontend.configuration.RateLimiterFactory
import com.francisbailey.summitsearch.frontend.configuration.ThrottlingConfiguration
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import javax.servlet.http.HttpServletRequest

@Service
class ThrottlingService(
    private val rateLimiterFactory: RateLimiterFactory,
    throttlingConfiguration: ThrottlingConfiguration
) {
    private val apiThrottleMap = hashMapOf<String, RateLimiterRegistry>()

    init {
        throttlingConfiguration.apiRequestPerSecondMap.forEach {
            apiThrottleMap[it.key] = rateLimiterFactory.build(it.value)
        }
    }

    fun shouldThrottle(request: HttpServletRequest): Boolean {
        val throttleKey = request.getHeader("x-forwarded-for") ?: defaultThrottleKey
        val apiRateLimitKey = rateLimiterKey(HttpMethod.valueOf(request.method), request.servletPath)

        val rateLimiter = apiThrottleMap[apiRateLimitKey]?.rateLimiter(throttleKey)

        return rateLimiter?.acquirePermission()?.not() ?: false
    }

    companion object {
        fun rateLimiterKey(method: HttpMethod, path: String): String {
            return "$method-$path"
        }

        const val defaultThrottleKey = "*"
    }

}