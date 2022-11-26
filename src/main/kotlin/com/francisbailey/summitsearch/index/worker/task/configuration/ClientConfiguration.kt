package com.francisbailey.summitsearch.index.worker.task.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Duration


@Configuration
open class ClientConfiguration(
    private val environment: Environment
) {

    @Bean
    open fun sqsClient(): SqsClient {
        return SqsClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .build()
    }

    @Bean
    open fun httpClient(): HttpClient {
       return HttpClient(CIO) {
            install(UserAgent) {
                agent = CRAWLING_AGENT
            }
            install(HttpRequestRetry) {
               retryOnServerErrors(3)
                exponentialDelay()
            }
            engine {
                requestTimeout = Duration.ofSeconds(3).toMillis()
            }
        }
    }

    @Bean
    open fun summitSearchIndexService(): SummitSearchIndexService {
        return SummitSearchIndexServiceFactory.build(
            SearchIndexServiceConfiguration(
            fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
            username = environment.getRequiredProperty("ES_USERNAME"),
            password = environment.getRequiredProperty("ES_PASSWORD"),
            endpoint = "localhost"
        )).also {
            it.createIndexIfNotExists()
        }
    }

    companion object {
        const val CRAWLING_AGENT = "SummitSearch Crawler"
    }

}