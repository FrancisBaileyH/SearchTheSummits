package com.francisbailey.summitsearch.frontend.configuration

import com.francisbailey.summitsearch.indexservice.SearchIndexServiceConfiguration
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexServiceFactory
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
open class ClientConfiguration(
    private val environment: Environment
) {

    private val log = KotlinLogging.logger {}

    /**
     * @TODO - use dev/prod profiles for defining this
     */
    @Bean
    open fun summitSearchIndexService(): SummitSearchIndexService {
        return SummitSearchIndexServiceFactory.buildInsecureLocal(SearchIndexServiceConfiguration(
            fingerprint = environment.getRequiredProperty("ES_FINGERPRINT"),
            username = environment.getRequiredProperty("ES_USERNAME"),
            password = environment.getRequiredProperty("ES_PASSWORD"),
            endpoint = environment.getRequiredProperty("ES_ENDPOINT")
        ).also {
            log.info { "Starting index client with config: $it" }
        })
    }
}
