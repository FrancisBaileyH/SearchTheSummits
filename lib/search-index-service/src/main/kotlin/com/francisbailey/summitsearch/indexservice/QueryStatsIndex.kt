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

    fun pushStats(request: QueryStatsPutRequest) {
        require(request.stats.size <= MAX_STATS_PER_UPDATE) {
            "Exceeded number of stats to push on bulk. Value: ${request.stats.size}. Max: $MAX_STATS_PER_UPDATE"
        }

        val indexOperations = request.stats.map {
            BulkOperation.of { operation ->
                operation.index { indexOp ->
                    indexOp.document(QueryStatMapping(
                        timestamp = it.timestamp,
                        query = it.query,
                        totalHits = it.totalHits,
                        page = it.page,
                        type = it.type,
                        sort = it.sort,
                        index = it.index
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
                        QueryStatMapping::timestamp.name to Property.of { property ->
                            property.date(DateProperty.Builder().build())
                        }
                    ))
                }
            })
        }
    }

    companion object {
        const val INDEX_NAME = "query-stats-index"
        const val MAX_STATS_PER_UPDATE = 120
    }
}


internal data class QueryStatMapping(
    val timestamp: Long,
    val query: String,
    val totalHits: Long,
    val page: Long?,
    val type: String?,
    val sort: String?,
    val index: String?
)

data class QueryStatsPutRequest(
    val stats: List<QueryStat>
)

data class QueryStat(
    val timestamp: Long,
    val query: String,
    val totalHits: Long,
    val page: Long?,
    val type: String,
    val sort: String,
    val index: String
)