package com.francisbailey.summitsearch.indexer.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.TransportUtils
import co.elastic.clients.transport.rest_client.RestClientTransport
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient
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
    open fun elasticSearchClient(): ElasticsearchClient {
        val fingerprint = environment.getRequiredProperty("ES_FINGERPRINT")
        val username = environment.getRequiredProperty("ES_USERNAME")
        val password = environment.getRequiredProperty("ES_PASSWORD")

        val sslContext = TransportUtils.sslContextFromCaFingerprint(fingerprint)
        val credsProv = BasicCredentialsProvider().apply {
            this.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
        }

        return ElasticsearchClient(
            RestClientTransport(
               RestClient.builder(
                    HttpHost(
                        "localhost",
                        9200,
                        "https"
                    )
                ).setHttpClientConfigCallback {
                    it.setSSLContext(sslContext)
                    it.setDefaultCredentialsProvider(credsProv)
                }.build(),
                JacksonJsonpMapper()
            )
        )
    }

    companion object {
        const val CRAWLING_AGENT = "SummitSearch Crawler"
    }

}