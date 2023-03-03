package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_DATE_FORMAT
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_LAST_NAME
import com.francisbailey.summitsearch.indexservice.common.SimpleQueryString
import com.francisbailey.summitsearch.indexservice.extension.*
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.net.URL

/**
 * There's definitely some opportunity to refactor this logic so that there's not so much
 * duplicated code between the ImageIndex and the SummitSearchIndex. However, I don't think
 * it's of the highest importance for now.
 */
class ImageIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    val indexName: String = INDEX_NAME
) {

    private val log = KotlinLogging.logger { }

    fun query(queryRequest: SummitSearchImagesQueryRequest): SummitSearchPaginatedResult<SummitSearchImage> {
        require(queryRequest.from in 0..MAX_FROM_VALUE) {
            "Query from value of: ${queryRequest.from} is invalid. Value must be from 0 to $MAX_FROM_VALUE"
        }

        require(queryRequest.term.length <= MAX_QUERY_TERM_SIZE) {
            "Query term must not contain more than $MAX_QUERY_TERM_SIZE characters"
        }

        log.info { "Querying images for: ${queryRequest.term}" }

        val summitSearchQuery = when(queryRequest.queryType) {
            SummitSearchQueryType.FUZZY -> buildFuzzyQuery(queryRequest)
            SummitSearchQueryType.STRICT -> buildSimpleQueryStringQuery(queryRequest)
        }

        val response = elasticSearchClient.search(SearchRequest.of {
            it.index(indexName)
            it.trackTotalHits { track ->
                track.enabled(true)
            }
            it.query(summitSearchQuery.query)
            if (queryRequest.sortType == SummitSearchSortType.BY_DATE) {
                it.sort { sort ->
                    sort.field { field ->
                        field.field(ImageMapping::referencingDocumentDate.name)
                        field.format(SORT_DATE_FORMAT)
                        field.missing(SORT_LAST_NAME)
                        field.order(SortOrder.Desc)
                    }
                }
            }
            it.source { sourceConfig ->
                sourceConfig.fetch(true)
            }
            it.size(paginationResultSize)
            it.from(queryRequest.from)
        }, ImageMapping::class.java)

        return SummitSearchPaginatedResult(
            hits = response.hits().hits().map {
                 SummitSearchImage(
                     dataStoreReference = it.source()!!.dataStoreReference,
                     description = it.source()!!.description,
                     source = it.source()!!.source,
                     referencingDocument = it.source()!!.referencingDocument
                 )
            },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0,
            sanitizedQuery = summitSearchQuery.rawQueryString
        )
    }

    fun indexImage(request: SummitSearchImagePutRequest) {
        indexImage(ImageType.STANDARD, request)
    }

    private fun indexImage(type: ImageType, request: SummitSearchImagePutRequest) {
        log.info { "Indexing image: $type for source: ${request.source}" }

        val result = elasticSearchClient.index(IndexRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.normalizedSource))
            it.document(ImageMapping(
                type = type,
                source = request.source.toString(),
                dataStoreReference = request.dataStoreReference,
                description = Jsoup.clean(request.description, Safelist.none()),
                referencingDocument = request.referencingDocument.toString(),
                referencingDocumentDate = request.referencingDocumentDate,
                referencingDocumentHost = request.referencingDocument.host
            ))
        })

        log.info { "Saved image: ${request.source}: ${result.result()}" }
    }


    fun createIfNotExists() {
        if (!elasticSearchClient.indexExists(indexName)) {
            log.info { "Index: $indexName not found. Creating now." }
            elasticSearchClient.indices().create(CreateIndexRequest.of {
                it.index(indexName)
                it.mappings { mapping ->
                    mapping.properties(mapOf(
                        ImageMapping::referencingDocumentHost.name to Property.of { property ->
                            property.keyword { keyword ->
                                keyword.docValues(true)
                            }
                        },
                        ImageMapping::referencingDocument.name to Property.of { property ->
                            property.keyword { keyword ->
                                keyword.docValues(true)
                            }
                        },
                        ImageMapping::description.name to Property.of { property ->
                            property.text(TextProperty.Builder().build())
                        },
                        ImageMapping::referencingDocumentDate.name to Property.of { property ->
                            property.date(DateProperty.Builder().build())
                        }
                    ))
                }
                it.settings { settings ->
                    settings.analysis { analysis ->
                        analysis.analyzer(ANALYZER_NAME) { analyzer ->
                            analyzer.custom { custom ->
                                custom.tokenizer("standard")
                                custom.filter(
                                    listOf(
                                        "lowercase",
                                        SYNONYM_FILTER_NAME
                                    )
                                )
                            }
                        }
                        analysis.filter(SYNONYM_FILTER_NAME) { tokenFilter ->
                            tokenFilter.definition { definition ->
                                definition.synonymGraph { synonymGraph ->
                                    synonymGraph.expand(true)
                                    synonymGraph.lenient(false)
                                    synonymGraph.synonyms(SummitSearchSynonyms.synonyms)
                                }
                            }
                        }
                    }
                }
            })
        }
    }
    private fun buildFuzzyQuery(request: SummitSearchImagesQueryRequest): SummitSearchQuery {
        val query = Query.of { query ->
            query.match { match ->
                match.query(request.term)
                match.field(ImageMapping::description.name)
                match.analyzer(SummitSearchIndexService.ANALYZER_NAME)
                match.operator(Operator.And)
                match.minimumShouldMatch("100%")
                match.fuzziness("AUTO")
                match.maxExpansions(2)
            }
        }

        return SummitSearchQuery(rawQueryString = request.term, query = query)
    }

    private fun buildSimpleQueryStringQuery(request: SummitSearchImagesQueryRequest): SummitSearchQuery {
        val sanitizedQuery = SimpleQueryString(PHRASE_TERM_THRESHOLD, request.term).sanitizedQuery()

        val query = Query.of { query ->
            query.simpleQueryString { match ->
                match.query(sanitizedQuery)
                match.fields(
                    ImageMapping::description.name
                )
                match.minimumShouldMatch("100%")
                match.analyzer(ANALYZER_NAME)
                match.defaultOperator(Operator.And)
            }
        }

        return SummitSearchQuery(rawQueryString = sanitizedQuery, query = query)
    }

    companion object {
        const val ANALYZER_NAME = "standard_with_synonyms"
        const val SYNONYM_FILTER_NAME = "synonym_graph"
        const val INDEX_NAME = "summit-search-images"
        const val MAX_FROM_VALUE = 2000
        const val MAX_QUERY_TERM_SIZE = 100
        const val PHRASE_TERM_THRESHOLD = 2
    }
}


internal data class ImageMapping(
    val source: String,
    val dataStoreReference: String,
    val description: String,
    val referencingDocument: String,
    val referencingDocumentHost: String,
    val referencingDocumentDate: Long?,
    val type: ImageType
)

internal enum class ImageType {
    THUMBNAIL,
    STANDARD
}

data class SummitSearchImagePutRequest(
    val normalizedSource: URL,
    val source: URL,
    val dataStoreReference: String,
    val description: String,
    val referencingDocument: URL,
    val referencingDocumentDate: Long? = null
)
data class SummitSearchImagesQueryRequest(
    val term: String,
    val from: Int = 0,
    val sortType: SummitSearchSortType = SummitSearchSortType.BY_RELEVANCE,
    val queryType: SummitSearchQueryType = SummitSearchQueryType.STRICT
)
data class SummitSearchImage(
    val dataStoreReference: String,
    val description: String,
    val source: String,
    val referencingDocument: String
)