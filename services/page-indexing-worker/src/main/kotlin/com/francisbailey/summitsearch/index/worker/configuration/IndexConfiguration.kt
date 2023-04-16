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
        indexName = SummitSearchIndexes.documentIndexName,
        elasticSearchClient,
        paginationResultSize = 10,
        synonyms = SummitSearchIndexes.synonyms
    ).also {
        it.createIfNotExists()
    }

    @Bean
    open fun imageIndexService() = ImageIndexService(
        indexName = SummitSearchIndexes.imageIndexName,
        elasticSearchClient,
        paginationResultSize = 30,
        synonyms = SummitSearchIndexes.synonyms
    ).also {
        it.createIfNotExists()
    }
}