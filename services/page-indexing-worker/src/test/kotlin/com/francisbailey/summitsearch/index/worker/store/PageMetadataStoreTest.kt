package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.store.PageMetadataStoreItem
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class PageMetadataStoreTest {

    @Test
    fun `canRefresh returns false if time elapsed is less than refresh duration`() {
        val refreshDuration = Duration.ofSeconds(10)
        val metadata = PageMetadataStoreItem(
            pageUrl = "http://test.com",
            taskId = "123",
            lastVisitTime = Instant.now().toEpochMilli()
        )

        assertFalse(metadata.canRefresh(refreshDuration))
    }

    @Test
    fun `canRefresh returns true if time elapsed is greater than refresh duration`() {
        val refreshDuration = Duration.ofSeconds(10)

        val metadata = PageMetadataStoreItem(
            pageUrl = "http://test.com",
            taskId = "123",
            lastVisitTime = Instant.now().minus(refreshDuration.plusSeconds(1)).toEpochMilli()
        )

        assertTrue(metadata.canRefresh(refreshDuration))
    }

    @Test
    fun `canRefresh returns false if refresh duration is 0`() {
        val refreshDuration = Duration.ofSeconds(0)

        val metadata = PageMetadataStoreItem(
            pageUrl = "http://test.com",
            taskId = "123",
            lastVisitTime = Instant.now().minus(refreshDuration.plusSeconds(1)).toEpochMilli()
        )

        assertFalse(metadata.canRefresh(refreshDuration))
    }

}