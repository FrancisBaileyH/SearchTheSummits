package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import com.francisbailey.summitsearch.services.common.RegionConfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import redis.clients.jedis.JedisPooled
import redis.clients.jedis.UnifiedJedis
import java.time.Duration

@Configuration
open class HttpEngineConfiguration {
    @Bean
    open fun httpClientEngine() = CIO.create {
        requestTimeout = Duration.ofSeconds(1).toMillis()
        endpoint.socketTimeout = Duration.ofSeconds(1).toMillis()
    }
}


@Configuration
open class ClientConfiguration(
    private val environment: Environment,
    private val regionConfig: RegionConfig
) {

    private val log = KotlinLogging.logger {}

    @Bean
    open fun summitSearchIndexService(): SummitSearchIndexService {
        return when {
            regionConfig.isProd -> SummitSearchIndexServiceFactory.build(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
                    paginationResultSize = 10
                ))
            else -> SummitSearchIndexServiceFactory.buildInsecureLocal(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
                    paginationResultSize = 10
                ))
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
    open fun httpClient(httpClientEngine: HttpClientEngine): HttpClient {
        return HttpClient(httpClientEngine) {
            install(HttpRequestRetry) {
                noRetry()
            }
            followRedirects = false
            expectSuccess = true
        }
    }
}
