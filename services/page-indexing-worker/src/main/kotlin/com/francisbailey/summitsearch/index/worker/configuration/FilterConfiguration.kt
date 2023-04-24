package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.filter.definitions.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class FilterConfiguration(
    private val siteProcessingConfigurations: Set<SiteProcessingConfiguration>
) {

    @Bean
    open fun linkDiscoveryFilterService(): DocumentFilterService {
        return DocumentFilterService(defaultChain = DefaultFilterChain).apply {
            siteProcessingConfigurations.forEach {
                it.discoveryFilter?.let { filter ->
                    addFilterChain(it.source, filter)
                }
            }
        }
    }

    @Bean
    open fun documentIndexingFilterService(): DocumentFilterService {
        return DocumentFilterService(defaultChain = DefaultIndexFilterChain).apply {
            siteProcessingConfigurations.forEach {
                it.indexingFilter?.let { filter ->
                    addFilterChain(it.source, filter)
                }
            }
        }
    }
}











