package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.MalformedURLException
import java.net.URL
import java.time.Instant
import java.util.UUID

data class ImageDiscovery(
    val source: String,
    val referencingURL: URL,
    val description: String,
    val pageCreationDate: Long? = null
)

class ImageDiscoveryTask(
    private val taskQueueClient: IndexingTaskQueueClient,
    private val discoveries: Set<ImageDiscovery>,
    private val associatedTask: IndexTask
): Runnable {

    private val log = KotlinLogging.logger { }

    override fun run() {
        val tasks: List<IndexTask> = discoveries.mapNotNull { discovery ->
            try {
                IndexTask(
                    source = associatedTask.source,
                    details = IndexTaskDetails(
                        id = UUID.randomUUID().toString(),
                        taskRunId = associatedTask.details.taskRunId,
                        pageUrl = URL(discovery.source),
                        submitTime = Instant.now().toEpochMilli(),
                        taskType = IndexTaskType.IMAGE,
                        refreshIntervalSeconds = associatedTask.details.refreshIntervalSeconds,
                        context = Json.encodeToString(
                            ImageTaskContext(
                                referencingURL = discovery.referencingURL,
                                description = discovery.description,
                                pageCreationDate = discovery.pageCreationDate
                            )
                        )
                    )
                )
            } catch (e: MalformedURLException) {
                log.debug(e) { "Bad image link: ${discovery.source}. Skipping" }
                null
            }
        }

        tasks.chunked(IndexingTaskQueueClient.MAX_MESSAGE_BATCH_SIZE).forEach {
            log.info { "Submitting images to queue for: ${associatedTask.details.pageUrl}" }
            taskQueueClient.addTasks(it)
        }
    }
}