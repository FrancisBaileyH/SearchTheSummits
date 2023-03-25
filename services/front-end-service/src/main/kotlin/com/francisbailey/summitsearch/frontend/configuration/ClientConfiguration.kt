package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.indexservice.*
import com.francisbailey.summitsearch.services.common.RegionConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class ClientConfiguration(
    private val environment: Environment,
    private val regionConfig: RegionConfig
) {

    open fun elasticSearchClientConfiguration(): ElasticSearchClientFactory.Configuration {
        val scheme = when {
            regionConfig.isProd -> "https"
            else -> "http"
        }

        return ElasticSearchClientFactory.Configuration(
            fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
            username = environment.getRequiredProperty("ES_USERNAME"),
            password = environment.getRequiredProperty("ES_PASSWORD"),
            endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
            scheme = scheme
        )
    }

    @Bean
    open fun elasticSearchClient() = ElasticSearchClientFactory.build(elasticSearchClientConfiguration())
}
