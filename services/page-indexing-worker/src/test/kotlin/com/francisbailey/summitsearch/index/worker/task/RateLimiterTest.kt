package com.francisbailey.summitsearch.index.worker.task

import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.LocalBucketBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class RateLimiterTest {
    private val bucket = mock<LocalBucket>()

    private val bucketBuilder = mock<LocalBucketBuilder> {
        on(mock.build()).thenReturn(bucket)
    }

    private val rateLimiter = DefaultRateLimiter<String>(bucketBuilder)

    @Test
    fun `should create new rate limiter for key when it does not exist`() {
        rateLimiter.tryConsume("newKey")
        verify(bucketBuilder).build()
    }

    @Test
    fun `should use cached rate limiter for key when it exists`() {
        rateLimiter.tryConsume("existingKey")
        verify(bucketBuilder).build()
        rateLimiter.tryConsume("existingKey")
        verifyNoMoreInteractions(bucketBuilder)
    }

    @Test
    fun `should return false if limit is exceeded`() {
        whenever(bucket.tryConsume(any())).thenReturn(false)
        assertFalse(rateLimiter.tryConsume("someKey"))
    }

    @Test
    fun `should return true if rate limit is not exceeded`() {
        whenever(bucket.tryConsume(any())).thenReturn(true)
        assertTrue(rateLimiter.tryConsume("someKey"))
    }
}