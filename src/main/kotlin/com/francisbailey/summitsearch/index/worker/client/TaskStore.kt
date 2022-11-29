package com.francisbailey.summitsearch.index.worker.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import org.springframework.stereotype.Service
import redis.clients.jedis.UnifiedJedis
import java.net.URL
import java.time.Instant
import javax.annotation.PreDestroy

@Service
class TaskStore(
    private val redisClient: UnifiedJedis
) {
    private val log = KotlinLogging.logger { }

    fun hasTask(taskRunId: String, pageUrl: URL): Boolean {
        val key = buildKey(taskRunId, pageUrl)
        log.info { "Checking if: $key exists." }

        val result = redisClient.get(key)

        return result != null && result != "nil"
    }

    fun saveTask(taskRunId: String, pageUrl: URL) {
        val key = buildKey(taskRunId, pageUrl)
        log.info { "Add value to $key" }
        redisClient.set(key, Json.encodeToString(TaskStoreItem(
            lastVisitTime = Instant.now().toEpochMilli(),
            pageUrl = pageUrl.toString(),
            taskId = taskRunId
        )))

        log.info { "Successfully saved $taskRunId and $pageUrl to $key" }
    }

    private fun buildKey(taskId: String, pageUrl: URL) = "$taskId-$pageUrl".take(MAX_KEY_LENGTH)

    @PreDestroy
    fun shutdown() {
        redisClient.close()
    }

    companion object {
        const val MAX_KEY_LENGTH = 100
    }
}


@Serializable
data class TaskStoreItem(
    val lastVisitTime: Long,
    val pageUrl: String,
    val taskId: String
)