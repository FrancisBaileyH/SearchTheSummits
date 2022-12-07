package com.francisbailey.summitsearch.services.common

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.core.env.Environment

class RegionConfigTest {

    private val environment = mock<Environment>()

    @Test
    fun `should return prod when environment is set to prod`() {
        whenever(environment.getProperty(any(), any())).thenReturn("PROD")

        val config = RegionConfig(environment)

        assertTrue(config.isProd)
        assertFalse(config.isBeta)
        assertFalse(config.isDev)
    }

    @Test
    fun `should return beta when environment is set to beta`() {
        whenever(environment.getProperty(any(), any())).thenReturn("BETA")

        val config = RegionConfig(environment)

        assertTrue(config.isBeta)
        assertFalse(config.isProd)
        assertFalse(config.isDev)
    }

    @Test
    fun `should return dev when environment is set to dev`() {
        whenever(environment.getProperty(any(), any())).thenReturn("DEV")

        val config = RegionConfig(environment)

        assertFalse(config.isBeta)
        assertFalse(config.isProd)
        assertTrue(config.isDev)
    }
}