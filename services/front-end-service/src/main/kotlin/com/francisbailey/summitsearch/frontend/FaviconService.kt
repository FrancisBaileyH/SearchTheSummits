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
import java.time.Instant
import java.util.Base64


/**
 * @TODO Cache TTL
*/
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
            localCache.getOrPut(pageUrl.host) {
                meterRegistry.counter("$METRIC_PREFIX-Local-CacheMiss").increment()
                getCachedFavicon(pageUrl)
            }
        }!!
    }

    private fun getCachedFavicon(pageUrl: URL): FaviconEntry {
        val cacheKey = buildKey(pageUrl)
        val refreshTime = Instant.now().toEpochMilli()

        try {
            val faviconEntry = jedis.get(cacheKey)

            return if (faviconEntry == null) {
                meterRegistry.counter("$METRIC_PREFIX-Redis-CacheMiss").increment()
                val entry = FaviconEntry(
                    lastUpdateSeconds = refreshTime,
                    imageData = getLiveFavicon(pageUrl)
                )

                jedis.set(cacheKey, Json.encodeToString(entry))
                meterRegistry.counter("$METRIC_PREFIX-Redis-CacheUpdate").increment()
                entry
            } else {
                meterRegistry.counter("$METRIC_PREFIX-Redis-CacheHit").increment()
                Json.decodeFromString(faviconEntry)
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to get Favicon for: $pageUrl" }
            return FaviconEntry(
                lastUpdateSeconds = refreshTime,
                imageData = fallbackFaviconData
            )
        }
    }

    private fun getLiveFavicon(pageUrl: URL): String {
        val faviconBaseUrl = "${pageUrl.protocol}://${pageUrl.host}"

        return try {
            val imageData = runBlocking {
                httpClient.get("$FAVICON_ENDPOINT=$faviconBaseUrl").readBytes()
            }

            Base64.getEncoder().encodeToString(imageData)
        } catch (e: Exception) {
            log.error(e) { "Failed to get live favicon for: $pageUrl" }
            fallbackFaviconData
        }
    }

    private fun buildKey(pageUrl: URL) = "$FAVICON_KEY_PREFIX-${pageUrl.host}"

    companion object {
        private const val METRIC_PREFIX = "FaviconService"
        private const val FAVICON_KEY_PREFIX = "Favicon-"
        private const val FAVICON_ENDPOINT = "https://t0.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&size=64&url"
    }
}

@Serializable
data class FaviconEntry(
    val imageData: String,
    val lastUpdateSeconds: Long
)