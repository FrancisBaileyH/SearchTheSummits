package com.francisbailey.summitsearch.frontend.throttling

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Component
@Order(1)
class ThrottlingFilter(
    private val throttlingService: ThrottlingService,
    private val meterRegistry: MeterRegistry
): Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (throttlingService.shouldThrottle(httpRequest)) {
            meterRegistry.counter("throttlingfilter.throttled").increment()
            httpResponse.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many requests from user")
        } else {
            chain.doFilter(request, response)
        }
    }
}

