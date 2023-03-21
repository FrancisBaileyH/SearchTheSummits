package com.francisbailey.summitsearch.index.coordinator.configuration

import com.francisbailey.summitsearch.index.coordinator.extension.exists
import com.francisbailey.summitsearch.index.coordinator.sources.IndexSource
import com.francisbailey.summitsearch.services.common.RegionConfig
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter

@Configuration
open class IndexSourceStoreConfiguration(
    regionConfig: RegionConfig,
    private val dynamoDbAsyncWaiter: DynamoDbAsyncWaiter,
    private val dynamoDbEnhancedAsyncClient: DynamoDbEnhancedAsyncClient
) {
        private val log = KotlinLogging.logger { }

        private val tableName = when {
            regionConfig.isProd -> "sts-index-source-store"
            else -> "sts-index-source-store-test"
        }

        @Bean
        open fun indexSourceStore(): DynamoDbAsyncTable<IndexSource> {
            val table = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(IndexSource::class.java))

            if (!table.exists()) {
                log.info { "Table: $tableName not found. Creating now" }
                table.createTable(
                    CreateTableEnhancedRequest.builder()
                        .provisionedThroughput(
                            ProvisionedThroughput.builder()
                                .writeCapacityUnits(1)
                                .readCapacityUnits(1)
                                .build())
                        .build()
                ).get()

                dynamoDbAsyncWaiter.waitUntilTableExists(
                    DescribeTableRequest.builder()
                        .tableName(tableName)
                        .build()
                ).get()

                log.info { "Table: $tableName created successfully." }
            }

            return table
        }

    }