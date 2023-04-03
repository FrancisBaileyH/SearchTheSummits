package com.francisbailey.summitsearch.index.worker.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.francisbailey.summitsearch.indexservice.ImageIndexService
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.services.common.SummitSearchIndexes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class IndexConfiguration(
    private val elasticSearchClient: ElasticsearchClient
) {

    @Bean
    open fun summitSearchIndexService() = DocumentIndexService(
        elasticSearchClient,
        paginationResultSize = 10,
        indexName = SummitSearchIndexes.documentIndexName,
        synonyms = SummitSearchIndexes.synonyms
    ).also {
        it.createIfNotExists()
    }

    @Bean
    open fun imageIndexService() = ImageIndexService(
        elasticSearchClient,
        paginationResultSize = 30,
        indexName = SummitSearchIndexes.imageIndexName,
        synonyms = SummitSearchIndexes.synonyms
    ).also {
        it.createIfNotExists()
    }
}