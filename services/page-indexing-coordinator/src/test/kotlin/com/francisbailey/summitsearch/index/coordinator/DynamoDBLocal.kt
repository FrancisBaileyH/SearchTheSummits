package com.francisbailey.summitsearch.index.coordinator

import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbAsyncWaiter
import java.net.URI

class DynamoDBLocal {

    private lateinit var container: GenericContainer<*>
    private lateinit var asyncClient: DynamoDbAsyncClient
    private lateinit var asyncWaiter: DynamoDbAsyncWaiter
    private lateinit var enhancedClient: DynamoDbEnhancedAsyncClient

    fun start(): DynamoDBLocal {
        val port = 8000
        val image: DockerImageName = DockerImageName.parse("amazon/dynamodb-local:latest")
        container = GenericContainer(image)
            .withExposedPorts(port)

        container.start()

        asyncClient = DynamoDbAsyncClient.builder()
            .endpointOverride(URI("http://localhost:${container.getMappedPort(port)}"))
            .region(Region.US_WEST_2)
            .credentialsProvider(StaticCredentialsProvider
                .create(AwsBasicCredentials
                    .create("ABC123", "ABC123")
                )
            )
            .build()

        asyncWaiter = DynamoDbAsyncWaiter.builder()
            .client(asyncClient)
            .build()

        enhancedClient = DynamoDbEnhancedAsyncClient.builder()
            .dynamoDbClient(asyncClient)
            .build()

        return this
    }

    fun asyncClient() = asyncClient

    fun asyncWaiter() = asyncWaiter

    fun enhancedAsyncClient() = enhancedClient

    fun stop() {
        container.close()
    }

    companion object {
        private var global: DynamoDBLocal? = null

        fun global(): DynamoDBLocal {
            if (global == null) {
                global = DynamoDBLocal().start()
            }

            return global!!
        }
    }
}