package com.francisbailey.summitsearch.frontend.throttling

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ThrottlingFilterTest {

    private val throttlingService = mock<ThrottlingService>()

    private val throttlingFilter = ThrottlingFilter(throttlingService)

    private val request = mock<HttpServletRequest>()

    private val response = mock<HttpServletResponse>()

    private val chain = mock<FilterChain>()

    @Test
    fun `sends 429 response if rate limit exceeded for request`() {
        whenever(throttlingService.shouldThrottle(request)).thenReturn(true)

        throttlingFilter.doFilter(request, response, chain)

        verifyNoInteractions(chain)
        verify(response).sendError(429, "Too many requests from user")
    }

    @Test
    fun `continues filter chain if rate limit not exceeded`() {
        whenever(throttlingService.shouldThrottle(request)).thenReturn(false)

        throttlingFilter.doFilter(request, response, chain)

        verifyNoInteractions(response)
        verify(chain).doFilter(request, response)
    }

}