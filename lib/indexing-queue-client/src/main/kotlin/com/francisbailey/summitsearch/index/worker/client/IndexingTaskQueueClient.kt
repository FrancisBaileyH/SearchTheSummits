package com.francisbailey.summitsearch.index.worker.client

import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient.Companion.MAX_MESSAGE_BATCH_SIZE
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


@Serializable
data class RedrivePolicy(
    val maxReceiveCount: Int,
    val deadLetterTargetArn: String
)

interface IndexingTaskQueuePollingClient {
    fun pollTask(queueName: String): IndexTask?
    fun deleteTask(indexTask: IndexTask)
    fun addTask(indexTask: IndexTask)
    fun addTasks(tasks: List<IndexTask>)
}

interface IndexingTaskQueueClient: IndexingTaskQueuePollingClient {
    fun listTaskQueues(prefix: String): Set<String>
    fun getTaskCount(queueName: String): Long
    fun queueExists(queueName: String): Boolean
    fun createQueue(queueName: String): String
    fun getQueueArn(queueName: String): String

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

    override fun listTaskQueues(prefix: String): Set<String> {
        val request = ListQueuesRequest.builder()
            .queueNamePrefix(prefix)
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

    override fun createQueue(queueName: String): String {
        val queueUrl = sqsClient.createQueue(CreateQueueRequest.builder()
            .queueName(queueName)
            .build()
        ).queueUrl()

        sqsClient.setQueueAttributes(SetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributes(mapOf(
                QueueAttributeName.REDRIVE_POLICY to Json.encodeToString(RedrivePolicy(
                    maxReceiveCount = 5,
                    deadLetterTargetArn = getQueueArn(DLQ_NAME)
                )),
                QueueAttributeName.VISIBILITY_TIMEOUT to "${Duration.ofMinutes(5).seconds}"
            ))
            .build()
        )

        return queueUrl
    }

    override fun getQueueArn(queueName: String): String {
        val attributes = sqsClient.getQueueAttributes(
            GetQueueAttributesRequest
                .builder()
                .queueUrl(queueName)
                .attributeNames(QueueAttributeName.QUEUE_ARN)
                .build()
        )

        return attributes.attributes()[QueueAttributeName.QUEUE_ARN]!!
    }

    companion object {
        const val MAX_RECEIVE_WAIT_TIME = 20
        const val DLQ_NAME = "IndexingQueue-DLQ"
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
    val entityUrl: URL,
    val submitTime: Long,
    val taskType: IndexTaskType,
    val entityTtl: Long, // 0 represents never refresh
    val context: String? = null
) {
    fun refreshDuration(): Duration = Duration.ofSeconds(entityTtl)

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
    val referencingTitle: String? = null,
    val description: String,
    val pageCreationDate: Long? = null
)


@Serializable
enum class IndexTaskType {
    HTML,
    THUMBNAIL,
    PDF,
    IMAGE,
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


