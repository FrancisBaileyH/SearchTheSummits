package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch._types.query_dsl.*
import co.elastic.clients.elasticsearch.core.*
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.search.HighlightField
import co.elastic.clients.elasticsearch.core.search.HighlighterFragmenter
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder
import co.elastic.clients.elasticsearch.core.search.HighlighterType
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.common.DefaultTextNormalizer
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.HIGHLIGHT_DELIMITER
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
import java.time.LocalDateTime
import java.time.ZoneOffset

class DocumentIndexService(
    val indexName: String,
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    private val synonyms: List<String> = emptyList(),
    private val clock: Clock = Clock.systemUTC(),
    private val textNormalizer: TextNormalizer = DefaultTextNormalizer()
) {
    private val log = KotlinLogging.logger { }

    val maxBulkIndexRequests = 100

    fun query(queryRequest: DocumentQueryRequest): PaginatedDocumentResult<DocumentQueryHit> {
        require(queryRequest.from in 0..MAX_FROM_VALUE) {
            "Query from value of: ${queryRequest.from} is invalid. Value must be from 0 to $MAX_FROM_VALUE"
        }

        require(queryRequest.term.length <= MAX_QUERY_TERM_SIZE) {
            "Query term must not contain more than $MAX_QUERY_TERM_SIZE characters"
        }

        log.info { "Querying for: ${queryRequest.term}" }

        val summitSearchQuery = when(queryRequest.queryType) {
            DocumentQueryType.FUZZY -> buildFuzzyQuery(queryRequest)
            DocumentQueryType.STRICT -> buildSimpleQueryStringQuery(queryRequest)
        }

        log.info { "Sanitized query: ${summitSearchQuery.rawQueryString}" }

        val response = elasticSearchClient.search(SearchRequest.of {
            it.index(indexName)
            it.trackTotalHits { track ->
                track.enabled(true)
            }
            it.query(summitSearchQuery.query)
            if (queryRequest.sortType == DocumentSortType.BY_DATE) {
                it.sort { sort ->
                    sort.field { field ->
                        field.field(DocumentMapping::pageCreationDate.name)
                        field.format(SORT_DATE_FORMAT)
                        field.missing(SORT_LAST_NAME)
                        field.order(SortOrder.Desc)
                    }
                }
            }
            it.fields(listOf(
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::source.name)
                },
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::title.name)
                },
                FieldAndFormat.of { field ->
                    field.field(DocumentMapping::thumbnails.name)
                }
            ))
            it.source { sourceConfig ->
                sourceConfig.fetch(false)
            }
            it.highlight { highlight ->
                highlight.numberOfFragments(HIGHLIGHT_FRAGMENT_COUNT)
                highlight.fragmentSize(HIGHLIGHT_FRAGMENT_SIZE)
                highlight.fields(mapOf(
                    DocumentMapping::seoDescription.name to HighlightField.Builder().build(),
                    DocumentMapping::paragraphContent.name to HighlightField.Builder().build(),
                    DocumentMapping::rawTextContent.name to HighlightField.Builder().build()
                ))
                highlight.order(HighlighterOrder.Score)
                highlight.noMatchSize(HIGHLIGHT_FRAGMENT_SIZE)
                highlight.type(HighlighterType.Plain)
                highlight.fragmenter(HighlighterFragmenter.Simple)
            }
            it.size(paginationResultSize)
            it.from(queryRequest.from)
        }, DocumentMapping::class.java)

        return PaginatedDocumentResult(
            hits = response.hits().hits()
                .filterNot { it.highlight().isEmpty() }
                .map {
                    // Order of precedence for matches
                    val highlightOptions = listOf(
                        it.highlight()[DocumentMapping::seoDescription.name]?.firstOrNull(),
                        it.highlight()[DocumentMapping::paragraphContent.name]?.firstOrNull(),
                        it.highlight()[DocumentMapping::rawTextContent.name]?.firstOrNull()
                    )

                    val highlight = highlightOptions.firstOrNull { highlight ->
                        highlight?.contains(HIGHLIGHT_DELIMITER) ?: false
                    } ?: highlightOptions.first { highlight ->
                        !highlight.isNullOrBlank()
                    }

                    DocumentQueryHit(
                        highlight = highlight!!.trim(),
                        source = it.stringField(DocumentMapping::source.name),
                        title = it.stringField(DocumentMapping::title.name),
                        thumbnails = it.listField(DocumentMapping::thumbnails.name)
                    )
                },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0,
            sanitizedQuery = summitSearchQuery.rawQueryString
        )
    }


    fun putThumbnails(request: DocumentThumbnailPutRequest) {
        log.info { "Updating thumbnails for: ${request.source}" }
        elasticSearchClient.update(UpdateRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.source))
            it.doc(mapOf(
                DocumentMapping::thumbnails.name to request.dataStoreReferences
            ))
        }, DocumentMapping::class.java)
    }

    fun pageExists(request: DocumentExistsRequest): Boolean {
        val result = elasticSearchClient.exists(ExistsRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.source))
        })

        return result.value()
    }

    /**
     * Unlike the put calls, this operation is index only meaning the entire
     * document gets re-indexed. This is an unfortunate limitation of the es java
     * client as the update call does not seem to work/be exposed.
     */
    fun indexPartitionedContent(requests: List<DocumentIndexRequest>) {
        val firstSource = generateIdFromUrl(requests.first().source)

        require(requests.size <= maxBulkIndexRequests) {
            "Too many requests: ${requests.size}. Max supported: $maxBulkIndexRequests"
        }

        require(requests.all { generateIdFromUrl(it.source) ==  firstSource }) {
            "Source document must be the same for all requests"
        }

        val indexOperations: List<BulkOperation> = requests.mapIndexed { partition, request ->
            BulkOperation.of { operation ->
                operation.index { indexOp ->
                    indexOp.id("${generateIdFromUrl(request.source)}-$partition")
                    indexOp.document(DocumentMapping(
                        title = textNormalizer.normalize(Jsoup.clean(request.title, Safelist.none())),
                        source = request.source,
                        host = request.source.host,
                        rawTextContent = textNormalizer.normalize(Jsoup.clean(request.rawTextContent, Safelist.none())),
                        paragraphContent = textNormalizer.normalize(Jsoup.clean(request.paragraphContent, Safelist.none())),
                        seoDescription = textNormalizer.normalize(Jsoup.clean(request.seoDescription, Safelist.none())),
                        pageCreationDate = request.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                        lastVisitTime = clock.instant().toEpochMilli()
                    ))
                }
            }
        }

        val result = elasticSearchClient.bulk(BulkRequest.of {
            it.index(indexName)
            it.operations(indexOperations)
            it.source { source ->
                source.fetch(false)
            }
        })

        log.info { "Bulk index result had error: ${result.errors()}" }
    }

    fun indexContent(request: DocumentPutRequest) {
        log.info { "Indexing content from: ${request.source}" }

        val result = elasticSearchClient.update(
            UpdateRequest.of {
                it.index(indexName)
                it.id(generateIdFromUrl(request.source))
                it.docAsUpsert(true)
                it.doc(
                    mapOf(
                        DocumentMapping::title.name to textNormalizer.normalize(Jsoup.clean(request.title, Safelist.none())),
                        DocumentMapping::source.name to request.source.toString(),
                        DocumentMapping::host.name to request.source.host,
                        DocumentMapping::rawTextContent.name to textNormalizer.normalize(Jsoup.clean(request.rawTextContent, Safelist.none())),
                        DocumentMapping::paragraphContent.name to textNormalizer.normalize(Jsoup.clean(request.paragraphContent, Safelist.none())),
                        DocumentMapping::seoDescription.name to textNormalizer.normalize(Jsoup.clean(request.seoDescription, Safelist.none())),
                        DocumentMapping::pageCreationDate.name to request.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                        DocumentMapping::lastVisitTime.name to clock.instant().toEpochMilli()
                    )
                )
            },
            DocumentMapping::class.java
        )

        log.info { "Result: ${result.result().name}" }
    }

    fun deletePageContents(request: DocumentDeleteRequest) {
        log.info { "Deleting: ${request.source} from index: $indexName" }
        val response = elasticSearchClient.delete(DeleteRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.source))
        })

        log.info { "Result: ${response.result()}" }
    }

    fun createIfNotExists() {
        if (!elasticSearchClient.indexExists(indexName)) {
            log.info { "Index: $indexName not found. Attempting to create it now" }

            val response = elasticSearchClient.indices().create(CreateIndexRequest.of {
                it.index(indexName)
                it.mappings { mapping ->
                    mapping.properties(mapOf(
                        DocumentMapping::host.name to Property.of { property ->
                            property.keyword { keyword ->
                                keyword.docValues(true)
                            }
                        },
                        DocumentMapping::paragraphContent.name to Property.of { property ->
                            property.text(TextProperty.Builder().build())
                        },
                        DocumentMapping::rawTextContent.name to Property.of { property ->
                            property.text(TextProperty.Builder().build())
                        },
                        DocumentMapping::pageCreationDate.name to Property.of { property ->
                            property.date(DateProperty.Builder().build())
                        }
                    ))
                }
                it.settings { settings ->
                    settings.analysis { analysis ->
                        analysis.analyzer(ANALYZER_NAME) { analyzer ->
                            analyzer.custom { custom ->
                                custom.tokenizer("standard")
                                custom.filter(listOf(
                                    "lowercase",
                                    SYNONYM_FILTER_NAME
                                ))
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

            log.info { "Result: ${response.acknowledged()}" }
        } else {
            log.info { "Index: $indexName already exists. Skipping creation." }
        }
    }

    private fun buildFuzzyQuery(request: DocumentQueryRequest): SearchQuery {
        val query = Query.of {  query ->
            query.multiMatch { match ->
                match.query(request.term)
                match.fields(
                    DocumentMapping::title.name.plus("^10"), // boost title the highest
                    DocumentMapping::rawTextContent.name,
                    DocumentMapping::seoDescription.name.plus("^3"), // boost the SEO description score
                    DocumentMapping::paragraphContent.name
                )
                match.analyzer(ANALYZER_NAME)
                match.operator(Operator.And)
                match.minimumShouldMatch("100%")
                match.fuzziness("AUTO")
                match.maxExpansions(2)
            }
        }

        return SearchQuery(rawQueryString = request.term, query = query)
    }

    private fun buildSimpleQueryStringQuery(request: DocumentQueryRequest): SearchQuery {
        val sanitizedQuery = SimpleQueryString(PHRASE_TERM_THRESHOLD, request.term).sanitizedQuery()

        val query = Query.of { query ->
            query.simpleQueryString { match ->
                match.query(sanitizedQuery)
                match.fields(
                    DocumentMapping::title.name.plus("^10"), // boost title the highest
                    DocumentMapping::rawTextContent.name,
                    DocumentMapping::seoDescription.name.plus("^3"), // boost the SEO description score
                    DocumentMapping::paragraphContent.name
                )
                match.minimumShouldMatch("100%")
                match.analyzer(ANALYZER_NAME)
                match.defaultOperator(Operator.And)
            }
        }

        return SearchQuery(rawQueryString = sanitizedQuery, query = query)
    }

   internal companion object {
       const val ANALYZER_NAME = "standard_with_synonyms"
       const val SYNONYM_FILTER_NAME = "synonym_graph"
       const val MAX_FROM_VALUE = 1_000
       const val MAX_QUERY_TERM_SIZE = 100
       const val HIGHLIGHT_FRAGMENT_SIZE = 200
       const val HIGHLIGHT_FRAGMENT_COUNT = 1
       const val PHRASE_TERM_THRESHOLD = 2
   }
}

internal data class SearchQuery(
    val rawQueryString: String,
    val query: Query
)

data class PaginatedDocumentResult<T>(
    val hits: List<T>,
    val next: Int = 0,
    val totalHits: Long = 0,
    val sanitizedQuery: String
)

data class DocumentQueryHit(
    val highlight: String,
    val source: String,
    val title: String,
    val thumbnails: List<String>?
)

enum class DocumentQueryType {
    FUZZY,
    STRICT
}

typealias DocumentPutRequest = DocumentIndexRequest
data class DocumentQueryRequest(
    val term: String,
    val from: Int = 0,
    val sortType: DocumentSortType = DocumentSortType.BY_RELEVANCE,
    val queryType: DocumentQueryType = DocumentQueryType.STRICT
)

enum class DocumentSortType {
    BY_DATE,
    BY_RELEVANCE
}

data class DocumentIndexRequest(
    val source: URL,
    val title: String,
    val rawTextContent: String,
    val paragraphContent: String,
    val seoDescription: String,
    val pageCreationDate: LocalDateTime? = null
)

data class DocumentDeleteRequest(
    val source: URL
)

data class DocumentThumbnailPutRequest(
    val source: URL,
    val dataStoreReferences: List<String>,
    val partition: Int? = null
)

data class DocumentExistsRequest(
    val source: URL,
    val partition: Int? = null
)

internal data class DocumentMapping(
    val host: String,
    val source: URL,
    val title: String,
    val seoDescription: String,
    val paragraphContent: String,
    val rawTextContent: String,
    val thumbnails: List<String>? = null,
    val pageCreationDate: Long? = null,
    val lastVisitTime: Long?
)