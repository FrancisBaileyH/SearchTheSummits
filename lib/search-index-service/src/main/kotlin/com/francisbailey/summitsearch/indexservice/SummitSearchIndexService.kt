package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat
import co.elastic.clients.elasticsearch.core.DeleteRequest
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.JsonpDeserializer
import mu.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import java.net.URL

class SummitSearchIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    private val indexName: String = SUMMIT_INDEX_NAME
) {
    private val log = KotlinLogging.logger { }

    fun query(queryRequest: SummitSearchQueryRequest): SummitSearchPaginatedResult {
        require(queryRequest.from in 0..MAX_FROM_VALUE) {
            "Query from value of: ${queryRequest.from} is invalid. Value must be from 0 to $MAX_FROM_VALUE"
        }

        require(queryRequest.term.length <= MAX_QUERY_TERM_SIZE) {
            "Query term must not contain more than $MAX_QUERY_TERM_SIZE characters"
        }

        log.info { "Querying for: ${queryRequest.term}" }

        val response = elasticSearchClient.search(
            SearchRequest.of {
                it.index(indexName)
                it.trackTotalHits { track ->
                    track.enabled(true)
                }
                it.query { query ->
                    query.matchPhrasePrefix { match ->
                        match.field(HtmlMapping::textContent.name)
                        match.query(queryRequest.term)
                    }
                }
                it.fields(listOf(
                    FieldAndFormat.of { field ->
                        field.field(HtmlMapping::source.name)
                    },
                    FieldAndFormat.of { field ->
                        field.field(HtmlMapping::title.name)
                    }
                ))
                it.source { sourceConfig ->
                    sourceConfig.fetch(false)
                }
                it.highlight { highlight ->
                    highlight.fragmentSize(HIGHLIGHT_FRAGMENT_SIZE)
                    highlight.fields(HtmlMapping::textContent.name) { highlightField ->
                        highlightField
                    }
                }
                it.size(paginationResultSize)
                it.from(queryRequest.from)
            },
            HtmlMapping::class.java
        )

        return SummitSearchPaginatedResult(
            hits = response.hits().hits().map {
                SummitSearchHit(
                    highlight = it.highlight()[HtmlMapping::textContent.name]!!.last(),
                    source = it.fields()[HtmlMapping::source.name]!!.deserialize(
                        JsonpDeserializer.arrayDeserializer(
                            JsonpDeserializer.stringDeserializer()
                        )).first(),
                    title = it.fields()[HtmlMapping::title.name]!!.deserialize(
                        JsonpDeserializer.arrayDeserializer(
                            JsonpDeserializer.stringDeserializer()
                        )).first()
                )
            },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0
        )
    }

    fun indexPageContents(request: SummitSearchIndexRequest) {
        log.info { "Indexing content from: ${request.source}" }

        request.htmlDocument.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        val textOnly = request.htmlDocument.body().text()
        val title = request.htmlDocument.title()

        val result = elasticSearchClient.index(
            IndexRequest.of {
                it.index(indexName)
                it.id(request.source.toString())
                it.document(HtmlMapping(
                    source = request.source,
                    title = title,
                    textContent = textOnly,
                    host = request.source.host
                ))
            }
        )

        log.info { "Result: ${result.result().name}" }
    }

    fun deletePageContents(request: SummitSearchDeleteIndexRequest) {
        log.info { "Deleting: ${request.source} from index: $indexName" }
        val response = elasticSearchClient.delete(DeleteRequest.of {
            it.index(indexName)
            it.id(request.source.toString())
        })

        log.info { "Result: ${response.result()}" }
    }

    fun indexExists(): Boolean {
        log.info { "Checking if index: $indexName exists" }
        val indexExistsResponse = elasticSearchClient.indices().exists(ExistsRequest.of{
            it.index(indexName)
        })

        return indexExistsResponse.value().also {
            log.info { "Index found result: $it" }
        }
    }

    fun createIndexIfNotExists() {
        if (!indexExists()) {
            log.info { "Index: $indexName not found. Attempting to create it now" }

            val response = elasticSearchClient.indices().create(CreateIndexRequest.of {
                it.index(indexName)
            })

            log.info { "Result: ${response.acknowledged()}" }
        } else {
            log.info { "Index: $indexName already exists. Skipping creation." }
        }
    }


   internal companion object {
        const val SUMMIT_INDEX_NAME = "summit-search-index"
        const val MAX_FROM_VALUE = 1_000
        const val MAX_QUERY_TERM_SIZE = 100
        const val HIGHLIGHT_FRAGMENT_SIZE = 100

        private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
            private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header", "table")

            override fun matches(root: Element, element: Element): Boolean {
                return excludedTags.contains(element.normalName())
            }
        }
   }
}

data class SummitSearchPaginatedResult(
    val hits: List<SummitSearchHit>,
    val next: Int = 0,
    val totalHits: Long = 0
)

data class SummitSearchHit(
    val highlight: String,
    val source: String,
    val title: String
)

data class SummitSearchQueryRequest(
    val term: String,
    val from: Int = 0,
)

data class SummitSearchIndexRequest(
    val source: URL,
    val htmlDocument: Document
)

data class SummitSearchDeleteIndexRequest(
    val source: URL
)

internal data class HtmlMapping(
    val host: String,
    val source: URL,
    val textContent: String,
    val title: String
)