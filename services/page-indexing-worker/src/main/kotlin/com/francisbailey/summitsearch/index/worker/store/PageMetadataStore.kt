package com.francisbailey.summitsearch.index.worker.store

import io.micrometer.core.instrument.MeterRegistry
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
    private val redisClient: UnifiedJedis,
    private val meter: MeterRegistry
) {
    private val log = KotlinLogging.logger { }

    fun getMetadata(pageUrl: URL): PageMetadataStoreItem? = meter.timer("$serviceName.get.latency").recordCallable {
        val key = buildKey(pageUrl)
        log.debug { "Fetching: $key" }
        val result: String? = redisClient.get(buildKey(pageUrl))

        if (result != null) {
            log.debug { "Key found: $key" }
            Json.decodeFromString<PageMetadataStoreItem>(result)
        } else {
            log.debug { "Store miss. No value found for key: $key" }
            null
        }
    }

    fun saveMetadata(taskRunId: String, pageUrl: URL) = meter.timer("$serviceName.put.latency").recordCallable{
        val key = buildKey(pageUrl)
        log.debug { "Add value to $key" }
        redisClient.set(key, Json.encodeToString(
            PageMetadataStoreItem(
                lastVisitTime = Instant.now().toEpochMilli(),
                pageUrl =  pageUrl.toString().lowercase(),
                taskId = taskRunId
            )
        ))

        log.debug { "Successfully saved $taskRunId and $pageUrl to $key" }
    }!!

    fun saveDiscoveryMetadata(discoveryHost: String) = meter.timer("$serviceName.put-discovery.latency").recordCallable {
        log.debug { "Adding discovery: $discoveryHost" }
        redisClient.sadd(DISCOVERY_METADATA_KEY, discoveryHost.lowercase())
        log.debug { "Successfully added discovery" }
    }

    fun getDiscoveryMetadata(): Set<String> {
        return redisClient.smembers(DISCOVERY_METADATA_KEY)
    }

    private fun buildKey(pageUrl: URL) = "Page-$pageUrl".take(MAX_KEY_LENGTH)

    companion object {
        const val DISCOVERY_METADATA_KEY = "Page-Discoveries-Metadata"
        const val MAX_KEY_LENGTH = 255
        const val serviceName = "page-metadata-store"
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