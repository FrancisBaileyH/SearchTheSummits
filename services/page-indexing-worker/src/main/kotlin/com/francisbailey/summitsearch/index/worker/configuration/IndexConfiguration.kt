package com.francisbailey.summitsearch.index.worker.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
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
    ).also {
        it.createIfNotExists()
    }

    @Bean
    open fun imageIndexService() = ImageIndexService(
        elasticSearchClient,
        paginationResultSize = 30
    ).also {
        it.createIfNotExists()
    }
}