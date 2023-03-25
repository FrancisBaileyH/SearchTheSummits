package com.francisbailey.summitsearch.frontend.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.francisbailey.summitsearch.indexservice.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class IndexConfiguration(
    private val elasticSearchClient: ElasticsearchClient
) {

    @Bean
    open fun summitSearchIndexService() = SummitSearchIndexService(
        elasticSearchClient,
        paginationResultSize = 10
    )

    @Bean
    open fun queryStatsService() = QueryStatsIndex(
        elasticSearchClient
    ).also {
        it.createIfNotExists()
    }

    @Bean
    open fun imageIndexService() = ImageIndexService(
        elasticSearchClient,
        paginationResultSize = 30
    )

    @Bean
    open fun previewImageResultsPerRequest(): Int = 6
}