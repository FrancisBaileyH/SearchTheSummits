package com.francisbailey.summitsearch.indexer.configuration

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsClient
import java.time.Duration

@Configuration
open class ClientConfiguration {

    @Bean
    fun sqsClient(): SqsClient {
        return SqsClient.builder()
            .build()
    }

    @Bean
    fun httpClient(): HttpClient {
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

    companion object {
        const val CRAWLING_AGENT = "SummitSearch Crawler"
    }

}