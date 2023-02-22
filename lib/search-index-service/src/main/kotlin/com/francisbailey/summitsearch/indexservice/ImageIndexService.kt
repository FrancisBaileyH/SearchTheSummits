package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.mapping.TextProperty
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import mu.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.net.URL

class ImageIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    private val indexName: String = INDEX_NAME
) {

    private val log = KotlinLogging.logger { }

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
                referencingDocument = generateIdFromUrl(request.referencingDocument),
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

    companion object {
        const val ANALYZER_NAME = "standard_with_synonyms"
        const val SYNONYM_FILTER_NAME = "synonym_graph"
        const val INDEX_NAME = "summit-search-images"
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

data class SummitSearchImage(
    val dataStoreReference: String,
    val description: String,
    val source: String,
    val referencingDocument: String?
)