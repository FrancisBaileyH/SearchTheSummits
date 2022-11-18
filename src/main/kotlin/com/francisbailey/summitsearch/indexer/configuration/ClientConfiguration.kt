package com.francisbailey.summitsearch.indexer.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
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

    @Bean
    fun elasticSearchClient(): ElasticsearchClient {
        return ElasticsearchClient(
            RestClientTransport(
                RestClient.builder(
                    HttpHost(
                        "localhost",
                        9200
                    )
                ).build(),
                JacksonJsonpMapper()
            )
        )
    }

    companion object {
        const val CRAWLING_AGENT = "SummitSearch Crawler"
    }

}