package com.francisbailey.summitsearch.index.worker.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*
import java.time.Duration


interface IndexingTaskQueuePollingClient {
    fun pollTask(queueName: String): IndexTask?
    fun deleteTask(indexTask: IndexTask)
    fun addTask(indexTask: IndexTask)
}

interface IndexingTaskQueueClient: IndexingTaskQueuePollingClient {
    fun listTaskQueues(): Set<String>
}

@Component
class SQSIndexingTaskQueueClient(
    private val sqsClient: SqsClient
): IndexingTaskQueueClient {

    override fun addTask(indexTask: IndexTask) {
        sqsClient.sendMessage(SendMessageRequest.builder()
            .queueUrl(indexTask.source)
            .messageBody(Json.encodeToString(indexTask.details))
            .build()
        )
    }

    override fun listTaskQueues(): Set<String> {
        val request = ListQueuesRequest.builder()
            .queueNamePrefix(INDEXING_QUEUE_PREFIX)
            .build()

        return sqsClient.listQueuesPaginator(request)
            .flatMap { it.queueUrls() }
            .toSet()
    }

    override fun pollTask(queueName: String): IndexTask? {
        val message = sqsClient.receiveMessage(
            ReceiveMessageRequest
                .builder()
                .queueUrl(queueName)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(MAX_RECEIVE_WAIT_TIME)
                .build()
        ).messages().firstOrNull()

        return message?.body()?.run {
            IndexTask(
                details = Json.decodeFromString(this),
                messageHandle = message.receiptHandle(),
                source = queueName
            )
        }
    }

    override fun deleteTask(indexTask: IndexTask) {
        sqsClient.deleteMessage(
            DeleteMessageRequest
                .builder()
                .queueUrl(indexTask.source)
                .receiptHandle(indexTask.messageHandle)
                .build()
        )
    }

    companion object {
        const val INDEXING_QUEUE_PREFIX = "IndexingQueue-"
        const val MAX_RECEIVE_WAIT_TIME = 20
    }
}


data class IndexTask(
    val messageHandle: String? = null,
    val source: String,
    val details: IndexTaskDetails
)


@Serializable
data class IndexTaskDetails(
    val id: String,
    val taskRunId: String,
    val pageUrl: String,
    val submitTime: Long,
    val refreshIntervalSeconds: Long // 0 represents never refresh
) {
    fun refreshDuration(): Duration = Duration.ofSeconds(refreshIntervalSeconds)
}


