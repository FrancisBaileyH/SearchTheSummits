package com.francisbailey.summitsearch.frontend.throttling

import com.francisbailey.summitsearch.frontend.configuration.ThrottlingConfiguration
import com.francisbailey.summitsearch.services.common.RateLimiter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.http.HttpMethod
import javax.servlet.http.HttpServletRequest


class ThrottlingServiceTest {

    private val rateLimiterFactory = mock<RateLimiterFactory>()

    private val throttlingConfig = mock<ThrottlingConfiguration>()

    private val request = mock<HttpServletRequest>()

    private val defaultRateLimiter = mock<RateLimiter<String>>()

    @Test
    fun `adds rate limiters on initilization and uses rate limiter on throttle`() {
        val api = ThrottlingService.rateLimiterKey(HttpMethod.POST, "/api/summits")
        val api2 = ThrottlingService.rateLimiterKey(HttpMethod.GET, "/api/summits")
        val ipAddress = "127.0.0.1"

        val rateLimiter = mock<RateLimiter<String>>()
        val rateLimiter2 = mock<RateLimiter<String>>()

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf(api to 1L, api2 to 5L))
        whenever(rateLimiterFactory.build(1L)).thenReturn(rateLimiter)
        whenever(rateLimiterFactory.build(5L)).thenReturn(rateLimiter2)

        whenever(request.method).thenReturn(HttpMethod.POST.name)
        whenever(request.servletPath).thenReturn("/api/summits")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)
        whenever(rateLimiter.tryConsume(any())).thenReturn(true)

        val throttlingService = ThrottlingService(rateLimiterFactory, defaultRateLimiter, throttlingConfig)

        verify(rateLimiterFactory).build(1L)
        verify(rateLimiterFactory).build(5L)

        assertFalse(throttlingService.shouldThrottle(request))
        verifyNoMoreInteractions(rateLimiterFactory)
        verifyNoInteractions(rateLimiter2)
        verify(rateLimiter).tryConsume(ipAddress)
    }

    @Test
    fun `uses default as fallback if no path matches`() {
        val ipAddress = "127.0.0.1"

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf())

        whenever(request.method).thenReturn(HttpMethod.DELETE.name)
        whenever(request.servletPath).thenReturn("/not/configured/api")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)

        whenever(defaultRateLimiter.tryConsume(any())).thenReturn(true)

        val throttlingService = ThrottlingService(rateLimiterFactory, defaultRateLimiter, throttlingConfig)

        assertFalse(throttlingService.shouldThrottle(request))
        verify(defaultRateLimiter).tryConsume(ipAddress)
    }

    @Test
    fun `returns true if rate limit exceeded`() {
        val api = ThrottlingService.rateLimiterKey(HttpMethod.POST, "/api/summits")
        val ipAddress = "127.0.0.1"

        val rateLimiter = mock<RateLimiter<String>>()

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf(api to 1L))
        whenever(rateLimiterFactory.build(1L)).thenReturn(rateLimiter)

        whenever(request.method).thenReturn(HttpMethod.POST.name)
        whenever(request.servletPath).thenReturn("/api/summits")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)
        whenever(rateLimiter.tryConsume(any())).thenReturn(false)

        val throttlingService = ThrottlingService(rateLimiterFactory, defaultRateLimiter, throttlingConfig)


        assertTrue(throttlingService.shouldThrottle(request))
        verify(rateLimiter).tryConsume(ipAddress)
    }

}