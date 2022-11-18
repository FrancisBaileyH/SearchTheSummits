package com.francisbailey.summitsearch.indexer.client

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.net.URL

@Service
class SearchIndexService(
    private val elasticSearchClient: ElasticsearchClient
) {
    private val log = KotlinLogging.logger { }


    fun indexPageContents(source: URL, htmlContent: String) {
        log.info { "Indexing content from: $source" }
        val result = elasticSearchClient.index(
            IndexRequest.of {
                it.index(SUMMIT_INDEX_NAME)
                it.id(source.toString())
                it.document(htmlContent)
            }
        )

        log.info { "Result: $result" }
    }


    fun createIndexIfNotExists() {
        val indexExistsResponse = elasticSearchClient.indices().exists(ExistsRequest.of{
            it.index(SUMMIT_INDEX_NAME)
        })

        if (!indexExistsResponse.value()) {
            log.info { "Index: $SUMMIT_INDEX_NAME not found. Attempting to create it now" }

            val response = elasticSearchClient.indices().create(CreateIndexRequest.of {
                it.index(SUMMIT_INDEX_NAME)
                it.settings { settings ->
                    settings.analysis { analysis ->
                        analysis.analyzer("htmlStripAnalyzer") { analyzer ->
                            analyzer.custom { custom ->
                                custom.filter(listOf("lowercase"))
                                custom.charFilter(listOf("html_strip"))
                                custom.tokenizer("standard")
                            }
                        }
                    }
                }
                it.mappings { mappings ->
                    mappings.properties("html") { property ->
                        property.text { text ->
                            text.analyzer("htmlStripAnalyzer")
                        }
                    }
                }
            })

            log.info { "Result: ${response.acknowledged()}" }
        } else {
            log.info { "Index: $SUMMIT_INDEX_NAME already exists. Skipping creation." }
        }
    }


    companion object {
        const val SUMMIT_INDEX_NAME = "SummitSearchIndex"
    }

}