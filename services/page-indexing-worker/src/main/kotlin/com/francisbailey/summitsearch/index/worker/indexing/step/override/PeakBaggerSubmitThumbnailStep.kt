package com.francisbailey.summitsearch.index.worker.indexing.step.override

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.extension.src
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.net.URL
import java.time.Instant


@Component
class PeakBaggerSubmitThumbnailStep(
    private val indexingTaskQueueClient: IndexingTaskQueueClient
): Step<Document> {

    override fun process(entity: PipelineItem<Document>, monitor: PipelineMonitor): PipelineItem<Document> {
        val imageSrc = entity.payload?.let {
            val candidateImages = it.select("img[src~=(?i)\\.(png|jpe?g)]")

            candidateImages.map { image ->
                image.src()
            }.firstOrNull { src ->
                src?.contains("peakbaggerblobs.blob.core.windows.net") ?: false
            }
        }

        imageSrc?.let {
            val details = entity.task.details
            monitor.dependencyCircuitBreaker.executeCallable {
                indexingTaskQueueClient.addTask(
                    IndexTask(
                        source = entity.task.source,
                        details = IndexTaskDetails(
                            id = details.id,
                            taskRunId = details.taskRunId,
                            pageUrl = URL(imageSrc),
                            submitTime = Instant.now().toEpochMilli(),
                            taskType = IndexTaskType.THUMBNAIL,
                            refreshIntervalSeconds = details.refreshIntervalSeconds,
                            context = Json.encodeToString(
                                ImageTaskContext(
                                    referencingURL = details.pageUrl,
                                    description = ""
                                )
                            )
                        )
                    )
                )
            }
        }

        return entity
    }

}