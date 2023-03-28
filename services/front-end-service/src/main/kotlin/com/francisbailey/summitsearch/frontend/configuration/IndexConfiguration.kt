package com.francisbailey.summitsearch.frontend.configuration

import co.elastic.clients.elasticsearch.ElasticsearchClient
import com.francisbailey.summitsearch.frontend.controller.SummitImagesController
import com.francisbailey.summitsearch.frontend.controller.SummitsController
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
        paginationResultSize = SummitsController.DOCUMENT_RESULTS_SIZE
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
        paginationResultSize = SummitImagesController.IMAGE_RESULT_SIZE
    )
}