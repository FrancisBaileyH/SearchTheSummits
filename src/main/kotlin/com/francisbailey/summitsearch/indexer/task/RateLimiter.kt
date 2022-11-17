package com.francisbailey.summitsearch.indexer.task

import io.github.bucket4j.local.LocalBucket
import io.github.bucket4j.local.LocalBucketBuilder

interface RateLimiter<T> {
    fun tryConsume(key: T): Boolean
}


class DefaultRateLimiter<T : Any>(
    private val bucketBuilder: LocalBucketBuilder
): RateLimiter<T> {
    /**
     * There is a memory leak here, as the map never gets purged. However, we only expect
     * a small number of entries, so this is tolerable for now.
     */
    private val rateLimiterMap = hashMapOf<T, LocalBucket>()

    override fun tryConsume(key: T): Boolean {
        val bucket = rateLimiterMap.getOrPut(key) {
            bucketBuilder.build()
        }

        return bucket.tryConsume(1)
    }
}