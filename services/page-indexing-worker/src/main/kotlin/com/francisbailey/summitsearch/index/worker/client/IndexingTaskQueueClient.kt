package com.francisbailey.summitsearch.index.worker.client

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*
import java.net.URL
import java.time.Duration


interface IndexingTaskQueuePollingClient {
    fun pollTask(queueName: String): IndexTask?
    fun deleteTask(indexTask: IndexTask)
    fun addTask(indexTask: IndexTask)
}

interface IndexingTaskQueueClient: IndexingTaskQueuePollingClient {
    fun listTaskQueues(): Set<String>
    fun getTaskCount(queueName: String): Long
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

    override fun getTaskCount(queueName: String): Long {
        val attributes = sqsClient.getQueueAttributes(
            GetQueueAttributesRequest
                .builder()
                .queueUrl(queueName)
                .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                .build()
        )

        return attributes.attributes()[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES]?.toLong() ?: 0L
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
    @Serializable(with = URLSerializer::class)
    val pageUrl: URL,
    val submitTime: Long,
    val taskType: IndexTaskType,
    val refreshIntervalSeconds: Long, // 0 represents never refresh
    val context: String? = null
) {
    fun refreshDuration(): Duration = Duration.ofSeconds(refreshIntervalSeconds)

    inline fun <reified T> getContext(): T? {
        return context?.let {
            Json.decodeFromString<T>(context)
        }
    }
}

@Serializable
data class ImageTaskContext(
    @Serializable(with = URLSerializer::class)
    val referencingURL: URL,
    val description: String
)


@Serializable
enum class IndexTaskType {
    HTML,
    THUMBNAIL,
    PDF,
    IMAGE
}

class URLSerializer : KSerializer<URL> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URL", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): URL {
        return URL(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeString(value.toString())
    }
}


