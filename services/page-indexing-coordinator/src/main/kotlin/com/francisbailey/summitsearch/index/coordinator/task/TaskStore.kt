package com.francisbailey.summitsearch.index.coordinator.task

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey


@Repository
class TaskStore(
    private val table: DynamoDbAsyncTable<Task>,
    private val meter: MeterRegistry
) {

    fun getTask(host: String): Task? = meter.timer("$service.get.latency").recordCallable {
        table.getItem(Key.builder()
            .partitionValue(host)
            .build()
        ).get()
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
    @get:DynamoDbPartitionKey
    var host: String = "",
    var id: String = "",
    var status: TaskStatus = TaskStatus.PENDING,
    var queueUrl: String = "",
    var monitorTimestamp: Long? = null,
    var seeds: Set<String> = emptySet(),
    var refreshInterval: Long = 0
)