package com.francisbailey.summitsearch.index.coordinator.task

import com.francisbailey.summitsearch.services.common.RegionConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey


@Repository
class TaskStore(
    regionConfig: RegionConfig,
    client: DynamoDbEnhancedAsyncClient,
    private val meter: MeterRegistry
) {
    private val tableName = when {
        regionConfig.isProd -> "sts-task-store"
        else -> "sts-task-store-test"
    }

    private val table = client.table(
        tableName,
        TableSchema.fromBean(Task::class.java)
    )

    fun getTask(host: String): Task? = meter.timer("$service.get.latency").recordCallable {
        val request = table.getItem(Key.builder().partitionValue(host).build())
        request.get()
    }

    fun getTasks(): List<Task> = meter.timer("$service.get-all.latency").recordCallable {
        val items = mutableListOf<Task>()

        table.scan { it.limit(MAX_TABLE_SCAN) }.items().subscribe {
            items.add(it)
        }.get()

        items
    }!!

    fun delete(task: Task) = meter.timer("$service.delete.latency").recordCallable {
        table.deleteItem(task).get()
    }

    fun save(task: Task) = meter.timer("$service.put.latency").recordCallable {
        table.putItem(task).get()
    }

    companion object {
        const val service = "task-store"
        const val MAX_TABLE_SCAN = 100
    }
}

enum class TaskStatus {
    PENDING,
    RUNNING,
    COMPLETED
}

@DynamoDbBean
data class Task(
    @get:DynamoDbSortKey
    var id: String? = null,
    @get:DynamoDbPartitionKey
    var host: String? = null,
    var status: TaskStatus? = null,
    var queueUrl: String? = null,
    var monitorTimestamp: Long? = null,
    var seeds: Set<String>? = null,
    var refreshInterval: Long? = null
)