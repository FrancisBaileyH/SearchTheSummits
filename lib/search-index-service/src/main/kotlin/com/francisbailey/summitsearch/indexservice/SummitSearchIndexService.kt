package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch._types.query_dsl.*
import co.elastic.clients.elasticsearch.core.DeleteRequest
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.search.HighlightField
import co.elastic.clients.elasticsearch.core.search.HighlighterOrder
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.JsonpDeserializer
import com.francisbailey.summitsearch.indexservice.extension.getSeoDescription
import com.francisbailey.summitsearch.indexservice.extension.words
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

        val term = queryRequest.term.replace(QUERY_SANITIZATION_REGEX, "")
        val words = term.words()
        val sanitizedQuery = StringBuilder()

        sanitizedQuery.append(words.take(PHRASE_TERM_THRESHOLD).joinToString(prefix = "\"", postfix = "\"", separator = " "))
        sanitizedQuery.append(words.drop(PHRASE_TERM_THRESHOLD).joinToString { " \"$it\"" })

        val response = elasticSearchClient.search(
            SearchRequest.of {
                it.index(indexName)
                it.trackTotalHits { track ->
                    track.enabled(true)
                }
                it.query { query ->
                    query.simpleQueryString { match ->
                        match.query(sanitizedQuery.toString())
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
                    highlight.order(HighlighterOrder.Score)
                }
                it.size(paginationResultSize)
                it.from(queryRequest.from)
            },
            HtmlMapping::class.java
        )

        return SummitSearchPaginatedResult(
            hits = response.hits().hits()
                .filterNot { it.highlight().isEmpty() }
                .map {
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

    fun indexPageContents(request: SummitSearchIndexRequest) {
        log.info { "Indexing content from: ${request.source}" }

        request.htmlDocument.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        val textOnly = request.htmlDocument.body().text()
        val paragraphContent = request.htmlDocument.body().select(HTML.Tag.P.toString()).text()
        val title = request.htmlDocument.title()
        val description = request.htmlDocument.getSeoDescription() ?: ""

        val result = elasticSearchClient.index(
            IndexRequest.of {
                it.index(indexName)
                it.id(generateId(request.source))
                it.document(HtmlMapping(
                    title = title,
                    source = request.source,
                    host = request.source.host,
                    rawTextContent = textOnly,
                    paragraphContent = paragraphContent,
                    seoDescription = description
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
                                    synonymGraph.synonyms(DEFAULT_SYNONYMS)
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

   internal companion object {
       const val SUMMIT_INDEX_NAME = "summit-search-index"
       const val ANALYZER_NAME = "standard_with_synonyms"
       const val SYNONYM_FILTER_NAME = "synonym_graph"
       const val MAX_FROM_VALUE = 1_000
       const val MAX_QUERY_TERM_SIZE = 100
       const val HIGHLIGHT_FRAGMENT_SIZE = 200
       const val HIGHLIGHT_FRAGMENT_COUNT = 1
       const val PHRASE_TERM_THRESHOLD = 2

       private val QUERY_SANITIZATION_REGEX = Regex("[^a-zA-Z0-9\\s]")

       private val DEFAULT_SYNONYMS = listOf(
           "mt., mt, mount",
           "mtn, mtn., mountain",
           "se, south east, southeast",
           "sw, south west, southwest",
           "nw, north west, northwest",
           "ne, north east, northeast",
           "bc, british columbia"
       )

       private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
           private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header")

           override fun matches(root: Element, element: Element): Boolean {
               return excludedTags.contains(element.normalName())
           }
       }

       fun generateId(url: URL): String {
           val uri = url.toURI()
           val query = uri.query?.split("&")?.sorted()?.joinToString(separator = "&", prefix = "?") ?: ""
           val path = uri.path?.removeSuffix("/") ?: ""
           return "${uri.host}$path$query"
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