package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch.core.DeleteRequest
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.search.HighlightField
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.JsonpDeserializer
import com.francisbailey.summitsearch.indexservice.extension.hasPunctuation
import com.francisbailey.summitsearch.indexservice.extension.normalizeWithoutSlash
import mu.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import java.net.URL
import javax.swing.text.html.HTML

class SummitSearchIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    val indexName: String = SUMMIT_INDEX_NAME
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
                    query.multiMatch { match ->
                        match.query(queryRequest.term)
                        match.type(TextQueryType.PhrasePrefix)
                        match.fields(
                            HtmlMapping::rawTextContent.name,
                            HtmlMapping::seoDescription.name.plus("^6"), // boost the SEO description score
                            HtmlMapping::paragraphContent.name
                        )
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
                    highlight.numberOfFragments(HIGHLIGHT_FRAGMENT_COUNT)
                    highlight.fragmentSize(HIGHLIGHT_FRAGMENT_SIZE)
                    highlight.fields(mapOf(
                        HtmlMapping::seoDescription.name to HighlightField.Builder().build(),
                        HtmlMapping::paragraphContent.name to HighlightField.Builder().build(),
                        HtmlMapping::rawTextContent.name to HighlightField.Builder().build()
                    ))
                }
                it.size(paginationResultSize)
                it.from(queryRequest.from)
            },
            HtmlMapping::class.java
        )

        return SummitSearchPaginatedResult(
            hits = response.hits().hits().map {
                // Order of precedence for matches
                val seoHighlight = it.highlight()[HtmlMapping::seoDescription.name]?.firstOrNull()
                val paragraphHighlight = it.highlight()[HtmlMapping::paragraphContent.name]?.firstOrNull()
                val rawTextHighlight = it.highlight()[HtmlMapping::rawTextContent.name]?.firstOrNull()

                SummitSearchHit(
                    highlight = seoHighlight ?: paragraphHighlight ?: rawTextHighlight!!,
                    source = it.stringField(HtmlMapping::source.name),
                    title = it.stringField(HtmlMapping::title.name)
                )
            },
            next = queryRequest.from + paginationResultSize,
            totalHits = response.hits().total()?.value() ?: 0
        )
    }

    /**
     * Add punctuation to fix missing/broken text. Up for debate if we should just leave
     * the text naturally as it is however.
     */
    fun indexPageContents(request: SummitSearchIndexRequest) {
        log.info { "Indexing content from: ${request.source}" }

        request.htmlDocument.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        val textOnly = request.htmlDocument.body().text()
        val paragraphContent = request.htmlDocument.body().select(HTML.Tag.P.toString()).text()

        val paragraphContentBuffer = StringBuffer()
        paragraphContentBuffer.append(paragraphContent)

        val title = request.htmlDocument.title()
        val description = request.htmlDocument.selectFirst("meta[name=description]")

        val result = elasticSearchClient.index(
            IndexRequest.of {
                it.index(indexName)
                it.id(request.source.normalizeWithoutSlash().toString())
                it.document(HtmlMapping(
                    title = title,
                    source = request.source,
                    host = request.source.host,
                    rawTextContent = textOnly,
                    paragraphContent = paragraphContent.toString(),
                    seoDescription = description?.attr("content") ?: ""
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
                        }
                    ))
                }
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
        const val HIGHLIGHT_FRAGMENT_SIZE = 200
        const val HIGHLIGHT_FRAGMENT_COUNT = 1

        private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
            private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header")

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
    val title: String,
    val seoDescription: String,
    val paragraphContent: String,
    val rawTextContent: String
)

internal fun Hit<HtmlMapping>.stringField(name: String): String {
    return this.fields()[name]!!.deserialize(
        JsonpDeserializer.arrayDeserializer(
            JsonpDeserializer.stringDeserializer()
        )).first()
}