package com.francisbailey.summitsearch.index.coordinator.sources

import com.francisbailey.summitsearch.services.common.RegionConfig
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant

@Repository
class IndexSourceStore(
    regionConfig: RegionConfig,
    client: DynamoDbEnhancedAsyncClient,
    private val meter: MeterRegistry
) {
    private val tableName = when {
        regionConfig.isProd -> "sts-index-source-store"
        else -> "sts-index-source-store-test"
    }

    private val table = client.table(
        tableName,
        TableSchema.fromBean(IndexSource::class.java)
    )

    fun getRefreshableSources() = meter.timer("$serviceName.get-refreshable.latency").recordCallable {
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
        }

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
    var host: String? = null,
    var seeds: Set<String>? = null,
    var nextUpdate: Long? = null,
    var refreshIntervalSeconds: Long ?= null,
    var documentTtl: Long? = null,
    var queueUrl: String? = null
)