package com.francisbailey.summitsearch.index.coordinator.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter

@Configuration
open class PersistenceConfiguration(
    private val asyncClient: DynamoDbAsyncClient
) {

    @Bean
    open fun dynamoDbEnhancedAsyncClient(): DynamoDbEnhancedAsyncClient {
        return DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(asyncClient)
            .build()
    }

    @Bean
    open fun dynamoDbAsyncWaiter(): DynamoDbAsyncWaiter {
        return DynamoDbAsyncWaiter.builder()
            .client(asyncClient)
            .build()
    }
}
