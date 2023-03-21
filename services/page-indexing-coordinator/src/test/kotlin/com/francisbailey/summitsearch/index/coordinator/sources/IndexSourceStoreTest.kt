package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.index.coordinator.DynamoDBLocal
import com.francisbailey.summitsearch.index.coordinator.configuration.IndexSourceStoreConfiguration
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Instant

class IndexSourceStoreTest {


    private val regionConfig = mock<RegionConfig> {
        on(mock.isProd).thenReturn(false)
    }

    private val configuration = IndexSourceStoreConfiguration(
        regionConfig,
        dynamoDBLocal.asyncWaiter(),
        dynamoDBLocal.enhancedAsyncClient()
    )

    private val indexSourceStore = IndexSourceStore(
        configuration.indexSourceStoreTable(),
        SimpleMeterRegistry()
    )

    @Test
    fun `items with nextUpdate time before current time are retrieved`() {
        val items = (0..3).map {
            IndexSource(
                host = "example$it.com",
                queueUrl = "some-queue-url",
                seeds = setOf("example.com"),
                refreshIntervalSeconds = 0,
                nextUpdate = Instant.now().minusSeconds(60).toEpochMilli(),
                documentTtl = 0
            )
        }

        items.forEach {
            indexSourceStore.save(it)
        }

        val result = indexSourceStore.getRefreshableSources().toSet()

        assertEquals(items.size, result.size)
        assertTrue(result.containsAll(result))

        items.forEach {
            indexSourceStore.save(it.apply {
                nextUpdate = Instant.now().plusSeconds(3600).toEpochMilli()
            })
        }

        assertTrue(indexSourceStore.getRefreshableSources().isEmpty())
    }


    companion object {
        private val dynamoDBLocal = DynamoDBLocal.global()
    }
}