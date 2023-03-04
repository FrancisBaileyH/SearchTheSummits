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
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.HIGHLIGHT_DELIMITER
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_DATE_FORMAT
import com.francisbailey.summitsearch.indexservice.common.ElasticSearchConstants.Companion.SORT_LAST_NAME
import com.francisbailey.summitsearch.indexservice.common.SimpleQueryString
import com.francisbailey.summitsearch.indexservice.extension.*
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.safety.Safelist
import org.jsoup.select.Evaluator
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.swing.text.html.HTML

class SummitSearchIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    val indexName: String = SUMMIT_INDEX_NAME
) {
    private val log = KotlinLogging.logger { }

    val maxBulkIndexRequests = 100

    fun query(queryRequest: SummitSearchQueryRequest): SummitSearchPaginatedResult<SummitSearchHit> {
        require(queryRequest.from in 0..MAX_FROM_VALUE) {
            "Query from value of: ${queryRequest.from} is invalid. Value must be from 0 to $MAX_FROM_VALUE"
        }

        require(queryRequest.term.length <= MAX_QUERY_TERM_SIZE) {
            "Query term must not contain more than $MAX_QUERY_TERM_SIZE characters"
        }

        log.info { "Querying for: ${queryRequest.term}" }

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
                            field.field(HtmlMapping::pageCreationDate.name)
                            field.format(SORT_DATE_FORMAT)
                            field.missing(SORT_LAST_NAME)
                            field.order(SortOrder.Desc)
                        }
                    }
                }
                it.fields(listOf(
                    FieldAndFormat.of { field ->
                        field.field(HtmlMapping::source.name)
                    },
                    FieldAndFormat.of { field ->
                        field.field(HtmlMapping::title.name)
                    },
                    FieldAndFormat.of { field ->
                        field.field(HtmlMapping::thumbnails.name)
                    }
                ))
                it.source { sourceConfig ->
                    sourceConfig.fetch(false)
                }
                it.highlight { highlight ->
                    highlight.numberOfFragments(HIGHLIGHT_FRAGMENT_COUNT)
                    highlight.fragmentSize(HIGHLIGHT_FRAGMENT_SIZE)
                    highlight.fields(mapOf(
                        HtmlMapping::seoDescription.name to HighlightField.Builder().build(),
                        HtmlMapping::paragraphContent.name to HighlightField.Builder().build(),
                        HtmlMapping::rawTextContent.name to HighlightField.Builder().build()
                    ))
                    highlight.order(HighlighterOrder.Score)
                    highlight.noMatchSize(HIGHLIGHT_FRAGMENT_SIZE)
                }
                it.size(paginationResultSize)
                it.from(queryRequest.from)
        }, HtmlMapping::class.java)

        return SummitSearchPaginatedResult(
            hits = response.hits().hits()
                .filterNot { it.highlight().isEmpty() }
                .map {
                    // Order of precedence for matches
                    val highlightOptions = listOf(
                        it.highlight()[HtmlMapping::seoDescription.name]?.firstOrNull(),
                        it.highlight()[HtmlMapping::paragraphContent.name]?.firstOrNull(),
                        it.highlight()[HtmlMapping::rawTextContent.name]?.firstOrNull()
                    )

                    val highlight = highlightOptions.firstOrNull { highlight ->
                        highlight?.contains(HIGHLIGHT_DELIMITER) ?: false
                    } ?: highlightOptions.first { highlight ->
                        !highlight.isNullOrBlank()
                    }

                    SummitSearchHit(
                        highlight = highlight!!,
                        source = it.stringField(HtmlMapping::source.name),
                        title = it.stringField(HtmlMapping::title.name),
                        thumbnails = it.listField(HtmlMapping::thumbnails.name)
                    )
                },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0,
            sanitizedQuery = summitSearchQuery.rawQueryString
        )
    }


    fun putThumbnails(request: SummitSearchPutThumbnailRequest) {
        log.info { "Updating thumbnails for: ${request.source}" }
        elasticSearchClient.update(UpdateRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.source))
            it.doc(mapOf(
                HtmlMapping::thumbnails.name to request.dataStoreReferences
            ))
        }, HtmlMapping::class.java)
    }

    fun pageExists(request: SummitSearchExistsRequest): Boolean {
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
    fun indexPartitionedContent(requests: List<SummitSearchIndexRequest>) {

        require(requests.size <= maxBulkIndexRequests) {
            "Too many requests: ${requests.size}. Max supported: $maxBulkIndexRequests"
        }

        require(requests.all { it.source == requests.first().source }) {
            "Source document must be the same for all requests"
        }

        val indexOperations: List<BulkOperation> = requests.mapIndexed { partition, request ->
            BulkOperation.of { operation ->
                operation.index { indexOp ->
                    indexOp.id("${generateIdFromUrl(request.source)}-$partition")
                    indexOp.document(HtmlMapping(
                        title = Jsoup.clean(request.title, Safelist.none()),
                        source = request.source,
                        host = request.source.host,
                        rawTextContent = Jsoup.clean(request.rawTextContent, Safelist.none()),
                        paragraphContent = Jsoup.clean(request.paragraphContent, Safelist.none()),
                        seoDescription = Jsoup.clean(request.seoDescription, Safelist.none()),
                        pageCreationDate = request.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
                    ))
                }
            }
        }

        elasticSearchClient.bulk(BulkRequest.of {
            it.index(indexName)
            it.operations(indexOperations)
            it.source { source ->
                source.fetch(false)
            }
        })
    }
    fun indexContent(request: SummitSearchPutRequest) {
        log.info { "Indexing content from: ${request.source}" }

        val result = elasticSearchClient.update(
            UpdateRequest.of {
                it.index(indexName)
                it.id(generateIdFromUrl(request.source))
                it.docAsUpsert(true)
                it.doc(
                    mapOf(
                        HtmlMapping::title.name to Jsoup.clean(request.title, Safelist.none()),
                        HtmlMapping::source.name to request.source.toString(),
                        HtmlMapping::host.name to request.source.host,
                        HtmlMapping::rawTextContent.name to Jsoup.clean(request.rawTextContent, Safelist.none()),
                        HtmlMapping::paragraphContent.name to Jsoup.clean(request.paragraphContent, Safelist.none()),
                        HtmlMapping::seoDescription.name to Jsoup.clean(request.seoDescription, Safelist.none()),
                        HtmlMapping::pageCreationDate.name to request.pageCreationDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
                    )
                )
            },
            HtmlMapping::class.java
        )

        log.info { "Result: ${result.result().name}" }
    }

    fun indexContent(request: SummitSearchPutHtmlPageRequest) {
        request.htmlDocument.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        val title = request.htmlDocument.title().ifBlank {
            request.source.host
        }

        val textOnly = request.htmlDocument.body().text()
        val paragraphContent = request.htmlDocument.body().select(HTML.Tag.P.toString()).text()
        val description = request.htmlDocument.getSeoDescription() ?: ""

        indexContent(
            SummitSearchIndexRequest(
                source = request.source,
                title = title,
                rawTextContent = textOnly,
                paragraphContent = paragraphContent,
                seoDescription = description,
                pageCreationDate = request.pageCreationDate
            )
        )
    }

    fun deletePageContents(request: SummitSearchDeleteIndexRequest) {
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
                        HtmlMapping::host.name to Property.of { property ->
                            property.keyword { keyword ->
                                keyword.docValues(true)
                            }
                        },
                        HtmlMapping::paragraphContent.name to Property.of { property ->
                            property.text(TextProperty.Builder().build())
                        },
                        HtmlMapping::rawTextContent.name to Property.of { property ->
                            property.text(TextProperty.Builder().build())
                        },
                        HtmlMapping::pageCreationDate.name to Property.of { property ->
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
                                    synonymGraph.synonyms(SummitSearchSynonyms.synonyms)
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

    private fun buildFuzzyQuery(request: SummitSearchQueryRequest): SummitSearchQuery {
        val query = Query.of {  query ->
            query.multiMatch { match ->
                match.query(request.term)
                match.fields(
                    HtmlMapping::title.name.plus("^10"), // boost title the highest
                    HtmlMapping::rawTextContent.name,
                    HtmlMapping::seoDescription.name.plus("^3"), // boost the SEO description score
                    HtmlMapping::paragraphContent.name
                )
                match.analyzer(ANALYZER_NAME)
                match.operator(Operator.And)
                match.minimumShouldMatch("100%")
                match.fuzziness("AUTO")
                match.maxExpansions(2)
            }
        }

        return SummitSearchQuery(rawQueryString = request.term, query = query)
    }

    private fun buildSimpleQueryStringQuery(request: SummitSearchQueryRequest): SummitSearchQuery {
        val sanitizedQuery = SimpleQueryString(PHRASE_TERM_THRESHOLD, request.term).sanitizedQuery()

        val query = Query.of { query ->
            query.simpleQueryString { match ->
                match.query(sanitizedQuery)
                match.fields(
                    HtmlMapping::title.name.plus("^10"), // boost title the highest
                    HtmlMapping::rawTextContent.name,
                    HtmlMapping::seoDescription.name.plus("^3"), // boost the SEO description score
                    HtmlMapping::paragraphContent.name
                )
                match.minimumShouldMatch("100%")
                match.analyzer(ANALYZER_NAME)
                match.defaultOperator(Operator.And)
            }
        }

        return SummitSearchQuery(rawQueryString = sanitizedQuery, query = query)
    }

   internal companion object {
       const val SUMMIT_INDEX_NAME = "summit-search-index"
       const val ANALYZER_NAME = "standard_with_synonyms"
       const val SYNONYM_FILTER_NAME = "synonym_graph"
       const val MAX_FROM_VALUE = 1_000
       const val MAX_QUERY_TERM_SIZE = 100
       const val HIGHLIGHT_FRAGMENT_SIZE = 200
       const val HIGHLIGHT_FRAGMENT_COUNT = 1
       const val PHRASE_TERM_THRESHOLD = 2


       private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
           private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header")

           override fun matches(root: Element, element: Element): Boolean {
               return excludedTags.contains(element.normalName())
           }
       }
   }
}

internal data class SummitSearchQuery(
    val rawQueryString: String,
    val query: Query
)

data class SummitSearchPaginatedResult<T>(
    val hits: List<T>,
    val next: Int = 0,
    val totalHits: Long = 0,
    val sanitizedQuery: String
)

data class SummitSearchHit(
    val highlight: String,
    val source: String,
    val title: String,
    val thumbnails: List<String>?
)

enum class SummitSearchQueryType {
    FUZZY,
    STRICT
}

typealias SummitSearchPutRequest = SummitSearchIndexRequest
data class SummitSearchQueryRequest(
    val term: String,
    val from: Int = 0,
    val sortType: SummitSearchSortType = SummitSearchSortType.BY_RELEVANCE,
    val queryType: SummitSearchQueryType = SummitSearchQueryType.STRICT
)

data class SummitSearchPutHtmlPageRequest(
    val source: URL,
    val htmlDocument: Document,
    val pageCreationDate: LocalDateTime? = null
)

enum class SummitSearchSortType {
    BY_DATE,
    BY_RELEVANCE
}

data class SummitSearchIndexRequest(
    val source: URL,
    val title: String,
    val rawTextContent: String,
    val paragraphContent: String,
    val seoDescription: String,
    val pageCreationDate: LocalDateTime? = null
)

data class SummitSearchDeleteIndexRequest(
    val source: URL
)

data class SummitSearchPutThumbnailRequest(
    val source: URL,
    val dataStoreReferences: List<String>,
    val partition: Int? = null
)

data class SummitSearchExistsRequest(
    val source: URL,
    val partition: Int? = null
)

internal data class HtmlMapping(
    val host: String,
    val source: URL,
    val title: String,
    val seoDescription: String,
    val paragraphContent: String,
    val rawTextContent: String,
    val thumbnails: List<String>? = null,
    val pageCreationDate: Long? = null
)