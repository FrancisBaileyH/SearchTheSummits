package com.francisbailey.summitsearch.frontend

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Service
import redis.clients.jedis.UnifiedJedis
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.Base64


@Service
class FaviconServicePrototype(
    private val jedis: UnifiedJedis,
    private val httpClient: HttpClient,
    private val meterRegistry: MeterRegistry,
    private val fallbackFaviconData: String
) {
    private val log = KotlinLogging.logger { }

    private val localCache = hashMapOf<String, FaviconEntry>()

    fun getFavicon(pageUrl: URL): FaviconEntry {
        return meterRegistry.timer("$METRIC_PREFIX-Latency").recordCallable {
            val faviconEntry = localCache[pageUrl.host]

            if (faviconEntry == null || faviconEntry.shouldRefresh(LOCAL_CACHE_TTL)) {
                meterRegistry.counter("$METRIC_PREFIX-Local-CacheMiss").increment()
                localCache[pageUrl.host] = getCachedFavicon(pageUrl)
            }

            localCache[pageUrl.host]
        }!!
    }

    private fun getCachedFavicon(pageUrl: URL): FaviconEntry {
        val cacheKey = buildKey(pageUrl)
        val refreshTime = Instant.now().toEpochMilli()

        try {
            val faviconEntry = jedis.get(cacheKey)?.let {
                Json.decodeFromString<FaviconEntry>(it)
            }

            return if (faviconEntry == null || faviconEntry.shouldRefresh(FAVICON_CACHE_TTL)) {
                meterRegistry.counter("$METRIC_PREFIX-Redis-CacheMiss").increment()
                val liveFaviconData = getLiveFavicon(pageUrl)
                val entry = FaviconEntry(
                    lastUpdateMillis = refreshTime,
                    imageData = liveFaviconData ?: faviconEntry?.imageData ?: fallbackFaviconData
                )

                liveFaviconData?.let {
                    jedis.set(cacheKey, Json.encodeToString(entry))
                    meterRegistry.counter("$METRIC_PREFIX-Redis-CacheUpdate").increment()
                }

                entry
            } else {
                meterRegistry.counter("$METRIC_PREFIX-Redis-CacheHit").increment()
                faviconEntry
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to get Favicon for: $pageUrl" }
            return FaviconEntry(
                lastUpdateMillis = refreshTime,
                imageData = fallbackFaviconData
            )
        }
    }

    private fun getLiveFavicon(pageUrl: URL): String? {
        return try {
            val faviconBaseUrl = "${pageUrl.protocol}://${pageUrl.host}"
            val endpoint = "$FAVICON_ENDPOINT=$faviconBaseUrl"
            val imageData = runBlocking {
                log.info { "Fetching live favicon from: $endpoint" }
                httpClient.get(endpoint).readBytes()
            }

            Base64.getEncoder().encodeToString(imageData)
        } catch (e: Exception) {
            log.error { "Failed to get live favicon for: $pageUrl because: ${e.localizedMessage}" }
            null
        }
    }

    private fun buildKey(pageUrl: URL) = "$FAVICON_KEY_PREFIX-${pageUrl.host}"

    companion object {
        private val LOCAL_CACHE_TTL: Duration = Duration.ofHours(1)
        private val FAVICON_CACHE_TTL: Duration = Duration.ofDays(7)
        private const val METRIC_PREFIX = "FaviconService"
        private const val FAVICON_KEY_PREFIX = "Favicon-"
        private const val FAVICON_ENDPOINT = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=64&url"
    }
}

@Serializable
data class FaviconEntry(
    val imageData: String,
    val lastUpdateMillis: Long
) {
    fun shouldRefresh(refreshInterval: Duration): Boolean {
        return Instant.ofEpochMilli(lastUpdateMillis).plus(refreshInterval) < Instant.now()
    }
}