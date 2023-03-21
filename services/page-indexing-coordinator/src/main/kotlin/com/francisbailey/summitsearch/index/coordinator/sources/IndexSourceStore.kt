package com.francisbailey.summitsearch.index.coordinator.sources

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

@Repository
class IndexSourceStore(
    private val table: DynamoDbAsyncTable<IndexSource>,
    private val meter: MeterRegistry
) {

    fun getRefreshableSources(): List<IndexSource> = meter.timer("$serviceName.get-refreshable.latency").recordCallable {
        val items = mutableListOf<IndexSource>()

        table.scan(ScanEnhancedRequest.builder()
            .filterExpression(Expression.builder()
                .expression("nextUpdate < :currentTime")
                .putExpressionValue(":currentTime", AttributeValue.fromN("${Instant.now().toEpochMilli()}"))
                .build()
            )
            .build()
        ).items().subscribe {
            items.add(it)
        }.get()

        items
    }!!

    fun save(source: IndexSource) = meter.timer("$serviceName.put.latency").recordCallable {
        table.putItem(source).get()
    }

    companion object {
        const val serviceName = "index-source-store"
    }
}


@DynamoDbBean
data class IndexSource(
    @get:DynamoDbPartitionKey
    var host: String = "",
    var seeds: Set<String> = emptySet(),
    var nextUpdate: Long = 0,
    var documentTtl: Long = 0,
    var queueUrl: String = ""
)