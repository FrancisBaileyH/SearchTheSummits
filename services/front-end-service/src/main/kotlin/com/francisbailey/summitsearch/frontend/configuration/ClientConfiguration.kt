package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class ClientConfiguration(
    private val environment: Environment
) {

    @Bean
    open fun summitSearchIndexService(): SummitSearchIndexService {
        return SummitSearchIndexServiceFactory.build(SearchIndexServiceConfiguration(
            fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
            username = environment.getRequiredProperty("ES_USERNAME"),
            password = environment.getRequiredProperty("ES_PASSWORD"),
            endpoint = "localhost"
        ))
    }
}