package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractor
import com.francisbailey.summitsearch.index.worker.extractor.strategy.DefaultContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.strategy.DefaultImageExtractorStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
open class ExtractorConfiguration(
    private val siteProcessingConfigurations: Set<SiteProcessingConfiguration>
) {

    @Bean
    open fun htmlContentExtractor() = ContentExtractor(
        defaultExtractor = DefaultContentExtractorStrategy(),
        extractorStrategyMap = siteProcessingConfigurations.filterNot {
            it.htmlContentSelector == null
        }.associate {
            it.source.host to it.htmlContentSelector!!
        }
    )

    @Bean
    open fun imageContentExtractor() = ContentExtractor(
        defaultExtractor = DefaultImageExtractorStrategy(),
        extractorStrategyMap = siteProcessingConfigurations.filterNot {
            it.imageContentSelector == null
        }.associate {
            it.source.host to it.imageContentSelector!!
        }
    )

}
