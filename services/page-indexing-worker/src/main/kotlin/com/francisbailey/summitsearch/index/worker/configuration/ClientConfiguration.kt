package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.htmldate.GoodEnoughHtmlDateGuesser
import com.francisbailey.htmldate.HtmlDateSearchConfiguration
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.UnifiedJedis
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI
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
    private val environment: Environment,
    private val regionConfig: RegionConfig
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
        return when {
            regionConfig.isProd -> SummitSearchIndexServiceFactory.build(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT")
                )
            )
            else -> SummitSearchIndexServiceFactory.build(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
                    scheme = "http"
                )
            )
        }.also {
            it.createIfNotExists()
        }
    }

    @Bean
    open fun imageIndexService(): ImageIndexService {
        return when {
            regionConfig.isProd -> SummitSearchIndexServiceFactory.buildImageIndex(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT")
                )
            )
            else -> SummitSearchIndexServiceFactory.buildImageIndex(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
                    scheme = "http"
                )
            )
        }.also {
            it.createIfNotExists()
        }
    }

    @Bean
    open fun redisClient(): UnifiedJedis {
        return JedisPooled(
            environment.getRequiredProperty("REDIS_ENDPOINT"),
            environment.getRequiredProperty("REDIS_PORT").toInt(),
            environment.getRequiredProperty("REDIS_USERNAME"),
            environment.getRequiredProperty("REDIS_PASSWORD")
        )
    }

    @Bean
    open fun s3Client(): S3Client {
        return S3Client.builder()
            .region(Region.of(environment.getRequiredProperty("S3_REGION")))
            .endpointOverride(URI(environment.getRequiredProperty("S3_ENDPOINT")))
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    environment.getRequiredProperty("S3_ACCESS_KEY"),
                    environment.getRequiredProperty("S3_SECRET_KEY")
                ))
            )
            .build()
    }

    @Bean
    open fun htmlDateGuesser(): GoodEnoughHtmlDateGuesser {
        return GoodEnoughHtmlDateGuesser.from(HtmlDateSearchConfiguration(
            useOriginalDate = true
        ))
    }

    companion object {
        const val CRAWLING_AGENT = "SearchTheSummitsBot"
    }

}