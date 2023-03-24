package com.francisbailey.summitsearch.index.worker.store

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import redis.clients.jedis.UnifiedJedis
import java.net.URL
import java.time.Duration
import java.time.Instant

class PageMetadataStoreTest {

    private val redisClient = mock<UnifiedJedis>()

    private val meter = SimpleMeterRegistry()

    private val store = PageMetadataStore(redisClient, meter)

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

    @Test
    fun `returns item if one is found`() {
        val url = URL("http://test.com")

        val item = PageMetadataStoreItem(
            pageUrl = url.toString(),
            taskId = "123",
            lastVisitTime = Instant.now().toEpochMilli()
        )

        whenever(redisClient.get("Page-$url")).thenReturn(Json.encodeToString(item))
        assertEquals(item, store.getMetadata(url))
    }

    @Test
    fun `returns null if no item is found`() {
        val url = URL("http://test.com")

        whenever(redisClient.get("Page-$url")).thenReturn(null)
        assertNull(store.getMetadata(url))
    }

    @Test
    fun `saves metadata with expected data`() {
        val taskId = "abc-123"
        val url = URL("http://test.com")

        store.saveMetadata("abc-123", url)

        verify(redisClient).set(eq("Page-$url"), org.mockito.kotlin.check {
            val metadata = Json.decodeFromString<PageMetadataStoreItem>(it)

            assertEquals(metadata.pageUrl, url.toString())
            assertEquals(metadata.taskId, taskId)
        })
    }

}