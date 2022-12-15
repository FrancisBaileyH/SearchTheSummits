package com.francisbailey.summitsearch.index.worker.metadata

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Service
import redis.clients.jedis.UnifiedJedis
import java.net.URL
import java.time.Duration
import java.time.Instant


@Service
class PageMetadataStore(
    private val redisClient: UnifiedJedis
) {
    private val log = KotlinLogging.logger { }

    fun getMetadata(pageUrl: URL): PageMetadataStoreItem? {
        val key = buildKey(pageUrl)
        log.info { "Fetching: $key" }
        val result: String? = redisClient.get(buildKey(pageUrl))

        return if (result != null) {
            log.info { "Key found: $key" }
            Json.decodeFromString<PageMetadataStoreItem>(result)
        } else {
            log.info { "Store miss. No value found for key: $key" }
            null
        }
    }

    fun saveMetadata(taskRunId: String, pageUrl: URL) {
        val key = buildKey(pageUrl)
        log.info { "Add value to $key" }
        redisClient.set(key, Json.encodeToString(
            PageMetadataStoreItem(
            lastVisitTime = Instant.now().toEpochMilli(),
            pageUrl = pageUrl.toString(),
            taskId = taskRunId
        )))

        log.info { "Successfully saved $taskRunId and $pageUrl to $key" }
    }

    fun saveDiscoveryMetadata(discoveryHost: String) {
        log.info { "Adding discovery: $discoveryHost" }
        redisClient.sadd(DISCOVERY_METADATA_KEY, discoveryHost)
        log.info { "Successfully added discovery" }
    }

    private fun buildKey(pageUrl: URL) = "Page-$pageUrl".take(MAX_KEY_LENGTH)

    companion object {
        const val DISCOVERY_METADATA_KEY = "Page-Discoveries-Metadata"
        const val MAX_KEY_LENGTH = 255
    }
}


@Serializable
data class PageMetadataStoreItem(
    val lastVisitTime: Long,
    val pageUrl: String,
    val taskId: String
) {
    fun canRefresh(refreshInterval: Duration): Boolean {
        return !refreshInterval.isZero
            && Instant.ofEpochMilli(lastVisitTime).plus(refreshInterval) < Instant.now()
    }
}