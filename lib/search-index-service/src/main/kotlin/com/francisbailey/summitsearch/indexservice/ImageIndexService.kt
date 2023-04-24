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
import com.francisbailey.summitsearch.indexservice.common.DefaultTextNormalizer
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_DATE_FORMAT
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_LAST_NAME
import com.francisbailey.summitsearch.indexservice.common.SimpleQueryString
import com.francisbailey.summitsearch.indexservice.common.TextNormalizer
import com.francisbailey.summitsearch.indexservice.extension.*
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.net.URL
import java.time.Clock

/**
 * There's definitely some opportunity to refactor this logic so that there's not so much
 * duplicated code between the ImageIndex and the SummitSearchIndex. However, I don't think
 * it's of the highest importance for now.
 */
class ImageIndexService(
    val indexName: String,
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    private val synonyms: List<String> = emptyList(),
    private val clock: Clock = Clock.systemUTC(),
    private val textNormalizer: TextNormalizer = DefaultTextNormalizer()
) {

    private val log = KotlinLogging.logger { }

    fun query(queryRequest: ImageQueryRequest): PaginatedDocumentResult<Image> {
        require(queryRequest.from in 0..MAX_FROM_VALUE) {
            "Query from value of: ${queryRequest.from} is invalid. Value must be from 0 to $MAX_FROM_VALUE"
        }

        require(queryRequest.term.length <= MAX_QUERY_TERM_SIZE) {
            "Query term must not contain more than $MAX_QUERY_TERM_SIZE characters"
        }

        log.info { "Querying images for: ${queryRequest.term}" }

        val summitSearchQuery = when(queryRequest.queryType) {
            DocumentQueryType.FUZZY -> buildFuzzyQuery(queryRequest)
            DocumentQueryType.STRICT -> buildSimpleQueryStringQuery(queryRequest)
        }

        val response = elasticSearchClient.search(SearchRequest.of {
            it.index(indexName)
            it.trackTotalHits { track ->
                track.enabled(true)
            }
            it.query(summitSearchQuery.query)
            if (queryRequest.sortType == DocumentSortType.BY_DATE) {
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
            it.size(queryRequest.paginationResultSize ?: paginationResultSize)
            it.from(queryRequest.from)
        }, ImageMapping::class.java)

        return PaginatedDocumentResult(
            hits = response.hits().hits().map {
                val source = it.source()!!
                 Image(
                     dataStoreReference = source.dataStoreReference,
                     description = source.description,
                     source = source.source,
                     referencingDocument = source.referencingDocument,
                     heightPx = source.heightPx,
                     widthPx = source.widthPx,
                     referencingDocumentTitle = source.referencingDocumentTitle
                 )
            },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0,
            sanitizedQuery = summitSearchQuery.rawQueryString
        )
    }

    fun indexImage(request: ImagePutRequest) {
        indexImage(ImageType.STANDARD, request)
    }

    private fun indexImage(type: ImageType, request: ImagePutRequest) {
        log.info { "Indexing image: $type for source: ${request.source}" }

        val result = elasticSearchClient.index(IndexRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.normalizedSource))
            it.document(ImageMapping(
                type = type,
                source = request.source.toString(),
                dataStoreReference = request.dataStoreReference,
                description = textNormalizer.normalize(Jsoup.clean(request.description, Safelist.none())),
                referencingDocument = request.referencingDocument.toString(),
                referencingDocumentDate = request.referencingDocumentDate,
                referencingDocumentHost = request.referencingDocument.host,
                heightPx = request.heightPx,
                widthPx = request.widthPx,
                lastVisitTime = clock.instant().toEpochMilli(),
                referencingDocumentTitle = request.referencingDocumentTitle?.run {
                    textNormalizer.normalize(Jsoup.clean(this, Safelist.none()))
                }
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
                                    synonymGraph.synonyms(synonyms)
                                }
                            }
                        }
                    }
                }
            })
        }
    }
    private fun buildFuzzyQuery(request: ImageQueryRequest): SearchQuery {
        val query = Query.of { query ->
            query.match { match ->
                match.query(request.term)
                match.field(ImageMapping::description.name)
                match.analyzer(ANALYZER_NAME)
                match.operator(Operator.And)
                match.minimumShouldMatch("100%")
                match.fuzziness("AUTO")
                match.maxExpansions(3)
            }
        }

        return SearchQuery(rawQueryString = request.term, query = query)
    }

    private fun buildSimpleQueryStringQuery(request: ImageQueryRequest): SearchQuery {
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

        return SearchQuery(rawQueryString = sanitizedQuery, query = query)
    }

    companion object {
        const val ANALYZER_NAME = "standard_with_synonyms"
        const val SYNONYM_FILTER_NAME = "synonym_graph"
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
    val referencingDocumentTitle: String?,
    val type: ImageType,
    val heightPx: Int,
    val widthPx: Int,
    val lastVisitTime: Long?
)

internal enum class ImageType {
    THUMBNAIL,
    STANDARD
}

data class ImagePutRequest(
    val normalizedSource: URL,
    val source: URL,
    val dataStoreReference: String,
    val description: String,
    val referencingDocument: URL,
    val referencingDocumentDate: Long? = null,
    val referencingDocumentTitle: String? = null,
    val heightPx: Int,
    val widthPx: Int
)

data class ImageQueryRequest(
    val term: String,
    val from: Int = 0,
    val sortType: DocumentSortType = DocumentSortType.BY_RELEVANCE,
    val queryType: DocumentQueryType = DocumentQueryType.STRICT,
    val paginationResultSize: Int? = null
)

data class Image(
    val dataStoreReference: String,
    val description: String,
    val source: String,
    val referencingDocument: String,
    val referencingDocumentTitle: String?,
    val heightPx: Int,
    val widthPx: Int
)