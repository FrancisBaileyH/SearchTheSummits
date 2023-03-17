package com.francisbailey.summitsearch.index.coordinator.configuration

import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import com.francisbailey.summitsearch.taskclient.SQSIndexingTaskQueueClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
open class ClientConfiguration {

    @Bean
    open fun indexingTaskQueueClient(): IndexingTaskQueueClient {
        return SQSIndexingTaskQueueClient(
            SqsClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build()
        )
    }

    @Bean
    open fun dynamoDbAsyncClient(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
    }
}