package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.UnifiedJedis
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.nio.charset.CodingErrorAction
import java.time.Duration
import kotlin.text.Charsets


@Configuration
open class HttpEngineConfiguration {

    @Bean
    open fun httpClientEngine() = CIO.create {
        requestTimeout = Duration.ofSeconds(3).toMillis()
    }
}


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

    /**
     * expectSuccess = true will turn on exceptions for non 20X responses
     */
    @Bean
    open fun httpClient(httpClientEngine: HttpClientEngine): HttpClient {
       return HttpClient(httpClientEngine) {
            expectSuccess = true
            install(UserAgent) {
                agent = CRAWLING_AGENT
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(3)
                exponentialDelay()
            }
            install(ContentEncoding) {
                gzip()
                deflate()
            }
           followRedirects = false
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

    @Bean
    open fun redisClient(): UnifiedJedis {
        return JedisPooled(
            "localhost",
            49153,
            environment.getRequiredProperty("REDIS_USERNAME"),
            environment.getRequiredProperty("REDIS_PASSWORD")
        )
    }

    companion object {
        const val CRAWLING_AGENT = "SummitSearch Crawler"
    }

}