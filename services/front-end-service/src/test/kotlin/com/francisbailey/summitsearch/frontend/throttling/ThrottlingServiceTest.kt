package com.francisbailey.summitsearch.frontend.throttling

import com.francisbailey.summitsearch.frontend.configuration.RateLimiterFactory
import com.francisbailey.summitsearch.frontend.configuration.ThrottlingConfiguration
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
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


    @Test
    fun `adds rate limiters on initilization and uses rate limiter on throttle`() {
        val api = ThrottlingService.rateLimiterKey(HttpMethod.POST, "/api/summits")
        val api2 = ThrottlingService.rateLimiterKey(HttpMethod.GET, "/api/summits")
        val ipAddress = "127.0.0.1"

        val rateLimiterRegistry = mock<RateLimiterRegistry>()
        val rateLimiterRegistry2 = mock<RateLimiterRegistry>()

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf(api to 1, api2 to 5))
        whenever(rateLimiterFactory.build(1)).thenReturn(rateLimiterRegistry)
        whenever(rateLimiterFactory.build(5)).thenReturn(rateLimiterRegistry2)

        val rateLimiter = mock<RateLimiter>()

        whenever(request.method).thenReturn(HttpMethod.POST.name)
        whenever(request.servletPath).thenReturn("/api/summits")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)
        whenever(rateLimiterRegistry.rateLimiter(any())).thenReturn(rateLimiter)
        whenever(rateLimiter.acquirePermission()).thenReturn(true)

        val throttlingService = ThrottlingService(rateLimiterFactory, throttlingConfig)

        verify(rateLimiterFactory).build(1)
        verify(rateLimiterFactory).build(5)

        assertFalse(throttlingService.shouldThrottle(request))
        verifyNoMoreInteractions(rateLimiterFactory)
        verifyNoInteractions(rateLimiterRegistry2)
        verify(rateLimiterRegistry).rateLimiter(ipAddress)
        verify(rateLimiter).acquirePermission()
    }

    @Test
    fun `skip rate limiting if no mapping is found`() {
        val ipAddress = "127.0.0.1"

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf())

        whenever(request.method).thenReturn(HttpMethod.DELETE.name)
        whenever(request.servletPath).thenReturn("/not/configured/api")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)


        val throttlingService = ThrottlingService(rateLimiterFactory, throttlingConfig)

        assertFalse(throttlingService.shouldThrottle(request))
    }

    @Test
    fun `returns true if rate limit exceeded`() {
        val api = ThrottlingService.rateLimiterKey(HttpMethod.POST, "/api/summits")
        val ipAddress = "127.0.0.1"

        val rateLimiter = mock<RateLimiter>()
        val rateLimiterRegistry = mock<RateLimiterRegistry>()

        whenever(throttlingConfig.apiRequestPerSecondMap).thenReturn(hashMapOf(api to 1))
        whenever(rateLimiterFactory.build(1)).thenReturn(rateLimiterRegistry)
        whenever(rateLimiterRegistry.rateLimiter(any())).thenReturn(rateLimiter)
        whenever(rateLimiter.acquirePermission()).thenReturn(false)

        whenever(request.method).thenReturn(HttpMethod.POST.name)
        whenever(request.servletPath).thenReturn("/api/summits")
        whenever(request.getHeader("x-forwarded-for")).thenReturn(ipAddress)

        val throttlingService = ThrottlingService(rateLimiterFactory, throttlingConfig)

        assertTrue(throttlingService.shouldThrottle(request))
        verify(rateLimiterRegistry).rateLimiter(ipAddress)
        verify(rateLimiter).acquirePermission()
    }

}