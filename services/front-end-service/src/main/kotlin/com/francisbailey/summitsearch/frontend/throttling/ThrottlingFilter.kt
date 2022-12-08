package com.francisbailey.summitsearch.frontend.throttling

import com.francisbailey.summitsearch.services.common.DefaultRateLimiter
import com.francisbailey.summitsearch.services.common.RateLimiter
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
@Order(1)
class ThrottlingFilter(
    private val throttlingService: ThrottlingService
): Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (throttlingService.shouldThrottle(httpRequest)) {
            httpResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests from user")
        } else {
            chain.doFilter(request, response)
        }
    }
}

@Service
class RateLimiterFactory {
    fun build(requestsPerSecond: Long): RateLimiter<String> {
        return DefaultRateLimiter(
            Bucket.builder()
                .addLimit(Bandwidth.simple(requestsPerSecond, Duration.ofSeconds(1)))
        )
    }
}

