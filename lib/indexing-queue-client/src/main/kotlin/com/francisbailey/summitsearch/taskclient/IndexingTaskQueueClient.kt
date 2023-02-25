package com.francisbailey.summitsearch.taskclient

import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient.Companion.MAX_MESSAGE_BATCH_SIZE
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.*
import java.net.URL
import java.time.Duration


interface IndexingTaskQueuePollingClient {
    fun pollTask(queueName: String): IndexTask?
    fun deleteTask(indexTask: IndexTask)
    fun addTask(indexTask: IndexTask)
    fun addTasks(tasks: List<IndexTask>)
}

interface IndexingTaskQueueClient: IndexingTaskQueuePollingClient {
    fun listTaskQueues(): Set<String>
    fun getTaskCount(queueName: String): Long
    fun queueExists(queueName: String): Boolean
    fun createQueue(queueName: String)

    companion object {
        const val MAX_MESSAGE_BATCH_SIZE = 10
    }
}


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

    /**
     * Tasks must all be for the same source
     */
    override fun addTasks(tasks: List<IndexTask>) {
        check(tasks.size <= MAX_MESSAGE_BATCH_SIZE) {
            "Tasks count must be 10 or less"
        }

        sqsClient.sendMessageBatch(SendMessageBatchRequest.builder()
            .queueUrl(tasks.first().source)
            .entries(tasks.map {
                SendMessageBatchRequestEntry.builder()
                    .messageBody(Json.encodeToString(it.details))
                    .id(it.details.id)
                    .build()
            })
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

    override fun queueExists(queueName: String): Boolean {
        return try {
            sqsClient.getQueueUrl(
                GetQueueUrlRequest
                    .builder()
                    .queueName(queueName)
                    .build()
            )
            true
        } catch (e: QueueDoesNotExistException) {
            false
        }
    }

    override fun createQueue(queueName: String) {
        val queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
            .queueName(queueName)
            .build()
        ).queueUrl()

        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributes(mapOf(
                QueueAttributeName.REDRIVE_POLICY to REDRIVE_POLICY,
                QueueAttributeName.VISIBILITY_TIMEOUT to "${Duration.ofMinutes(5).seconds}"
            ))
            .build()
        )
    }

    companion object {
        const val INDEXING_QUEUE_PREFIX = "IndexingQueue-"
        const val MAX_RECEIVE_WAIT_TIME = 20
        const val REDRIVE_POLICY = """
            {
                "maxReceiveCount": "5",
                "deadLetterTargetArn": "arn:aws:sqs:us-west-2:259609947632:IndexingQueue-DLQ"
            }
        """
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
    val description: String,
    val pageCreationDate: Long? = null
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


