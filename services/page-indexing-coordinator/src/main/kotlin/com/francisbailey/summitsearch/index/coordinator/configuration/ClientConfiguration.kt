package com.francisbailey.summitsearch.index.coordinator.configuration

import com.francisbailey.summitsearch.taskclient.IndexingTaskQueueClient
import com.francisbailey.summitsearch.taskclient.SQSIndexingTaskQueueClient
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.getRequiredProperty
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Clock
import java.time.Duration

@Configuration
open class HttpEngineConfiguration {

    @Bean
    open fun httpClientEngine() = CIO.create {
        requestTimeout = Duration.ofSeconds(30).toMillis()
        endpoint.socketTimeout = Duration.ofSeconds(30).toMillis()
    }

}

@Configuration
open class ClientConfiguration(
    environment: Environment
) {

    private val credentialsProvider = StaticCredentialsProvider.create(
        AwsBasicCredentials.create(
            environment.getRequiredProperty("COORDINATOR_ACCESS_ID"),
            environment.getRequiredProperty("COORDINATOR_ACCESS_KEY")
        )
    )

    @Bean
    open fun indexingTaskQueueClient(): IndexingTaskQueueClient {
        return SQSIndexingTaskQueueClient(
            SqsClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(credentialsProvider)
                .build()
        )
    }

    @Bean
    open fun dynamoDbAsyncClient(): DynamoDbAsyncClient {
        return DynamoDbAsyncClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(credentialsProvider)
            .build()
    }

    @Bean
    open fun httpClient(httpClientEngine: HttpClientEngine): HttpClient {
        return HttpClient(httpClientEngine) {
            install(UserAgent) {
                agent = "Page-Indexing-Coordinator"
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 1000
            }
            install(ContentNegotiation) {
                json()
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
            expectSuccess = true
            followRedirects = false
        }
    }

    @Bean
    open fun clock() = Clock.systemUTC()
}