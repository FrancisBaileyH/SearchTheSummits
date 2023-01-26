package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import com.francisbailey.summitsearch.services.common.RegionConfig
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

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
            else -> SummitSearchIndexServiceFactory.build(
                SearchIndexServiceConfiguration(
                    fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
                    username = environment.getRequiredProperty("ES_USERNAME"),
                    password = environment.getRequiredProperty("ES_PASSWORD"),
                    endpoint =  environment.getRequiredProperty("ES_ENDPOINT"),
                    scheme = "http",
                    paginationResultSize = 10
                ))
        }

    }
}
