package com.francisbailey.summitsearch.frontend.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.francisbailey.summitsearch.frontend.controller.SummitImagesController
import com.francisbailey.summitsearch.frontend.controller.SummitsController
import com.francisbailey.summitsearch.indexservice.*
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
        paginationResultSize = SummitsController.DOCUMENT_RESULTS_SIZE,
        synonyms = SummitSearchIndexes.synonyms
    )

    @Bean
    open fun queryStatsService() = QueryStatsIndex(
        elasticSearchClient
    ).also {
        it.createIfNotExists()
    }

    @Bean
    open fun imageIndexService() = ImageIndexService(
        indexName = SummitSearchIndexes.imageIndexName,
        elasticSearchClient,
        paginationResultSize = SummitImagesController.IMAGE_RESULT_SIZE,
        synonyms = SummitSearchIndexes.synonyms
    )
}