package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import mu.KotlinLogging

class QueryStatsIndex(
    private val elasticSearchClient: ElasticsearchClient,
    private val indexName: String = INDEX_NAME
) {

    private val log = KotlinLogging.logger { }

    /**
     * Check that query is not too long
     * Check that num of stats is not too many
     */
    fun pushStats(request: SummitSearchQueryStatsPutRequest) {
        val indexOperations = request.stats.map {
            BulkOperation.of { operation ->
                operation.index { indexOp ->
                    indexOp.document(QueryStat(
                        timestamp = it.timestamp,
                        query = it.query,
                        totalHits = it.totalHits,
                        page = it.page
                    ))
                }
            }
        }

        elasticSearchClient.bulk(
            BulkRequest.of {
                it.index(indexName)
                it.source { source ->
                    source.fetch(false)
                }
                it.operations(indexOperations)
            }
        ).also {
            log.info { "Bulk operation errors: ${it.errors()}" }
        }
    }

    fun createIfNotExists() {
        if (!elasticSearchClient.indexExists(indexName)) {
            log.info { "Index: $indexName not found. Creating now." }
            elasticSearchClient.indices().create(CreateIndexRequest.of{
                it.index(indexName)
                it.mappings { mapping ->
                    mapping.properties(mapOf(
                        QueryStat::timestamp.name to Property.of { property ->
                            property.date(DateProperty.Builder().build())
                        }
                    ))
                }
            })
        }
    }

    companion object {
        const val INDEX_NAME = "query-stats-index"
    }
}


internal data class QueryStat(
    val timestamp: Long,
    val query: String,
    val totalHits: Long,
    val page: Long?
)

data class SummitSearchQueryStatsPutRequest(
    val stats: List<SummitSearchQueryStat>
)

data class SummitSearchQueryStat(
    val timestamp: Long,
    val query: String,
    val totalHits: Long,
    val page: Long?
)