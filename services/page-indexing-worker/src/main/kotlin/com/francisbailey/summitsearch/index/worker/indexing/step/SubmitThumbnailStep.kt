package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.extension.getFigCaptionedImages
import com.francisbailey.summitsearch.index.worker.extension.getOGImage
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant

@Component
class SubmitThumbnailStep(
    private val indexingTaskQueueClient: IndexingTaskQueueClient
): Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val thumbnailCandidate = entity.payload?.document?.getOGImage()
            ?: entity.payload?.document?.getFigCaptionedImages()?.firstOrNull()

        thumbnailCandidate?.let {
            monitor.dependencyCircuitBreaker.executeCallable {
                val details = entity.task.details
                log.info { "Found thumbnail: ${thumbnailCandidate.imageSrc} on ${details.pageUrl}" }

                indexingTaskQueueClient.addTask(
                    IndexTask(
                        source = entity.task.source,
                        details = IndexTaskDetails(
                            id = details.id,
                            taskRunId = details.taskRunId,
                            pageUrl = URL(thumbnailCandidate.imageSrc),
                            submitTime = Instant.now().toEpochMilli(),
                            taskType = IndexTaskType.THUMBNAIL,
                            refreshIntervalSeconds = details.refreshIntervalSeconds,
                            context = Json.encodeToString(ImageTaskContext(
                                referencingURL = details.pageUrl,
                                description = thumbnailCandidate.caption
                            )
                        )
                    )
                ))
            }
        }

        return entity
    }
}