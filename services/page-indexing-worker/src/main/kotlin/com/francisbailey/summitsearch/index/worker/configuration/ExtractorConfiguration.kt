package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractor
import com.francisbailey.summitsearch.index.worker.extractor.strategy.DefaultContentExtractorStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class ExtractorConfiguration(
    siteProcessingConfigurations: SiteProcessingConfigurations
) {

    private val configurationStrategyMap = siteProcessingConfigurations.configurations.filterNot {
        it.htmlContentSelector == null
    }.associate {
        it.source.host to it.htmlContentSelector!!
    }

    @Bean
    open fun htmlContentExtractor() = ContentExtractor(
        defaultExtractor = DefaultContentExtractorStrategy(),
        configurationStrategyMap
    )

}
