package com.francisbailey.summitsearch.indexer.client

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ListQueuesRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

interface TaskQueuePollingClient {
    fun pollTask(queueName: String): IndexTask?
    fun deleteTask(indexTask: IndexTask)
}

interface TaskQueueClient: TaskQueuePollingClient {
    fun listTaskQueues(): Set<String>
}

@Component
class SQSTaskQueueClient(
    private val sqsClient: SqsClient
): TaskQueueClient {

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
                .build()
        ).messages().firstOrNull()

        return message?.body()?.run {
            IndexTask(
                details = Json.decodeFromString<IndexTaskDetails>(this),
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
    }
}


data class IndexTask(
    val messageHandle: String,
    val source: String,
    val details: IndexTaskDetails
)


@Serializable
data class IndexTaskDetails(
    val id: String,
    val pageUrl: String,
    val submitTime: Long
)


