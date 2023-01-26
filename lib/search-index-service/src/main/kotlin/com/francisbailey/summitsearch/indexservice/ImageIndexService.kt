package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import mu.KotlinLogging
import java.net.URL

class ImageIndexService(
    private val elasticSearchClient: ElasticsearchClient,
    private val paginationResultSize: Int,
    private val indexName: String = INDEX_NAME
) {

    private val log = KotlinLogging.logger { }

    fun fetchThumbnails(request: SummitSearchGetThumbnailsRequest): SummitSearchGetThumbnailsResponse {
        log.info { "Fetching thumbnails for referencing documents: ${request.referenceDocuments}" }
        val referenceDocuments = request.referenceDocuments.map {
            FieldValue.of(generateIdFromUrl(it))
        }

        val result = elasticSearchClient.search(SearchRequest.of{
            it.index(indexName)
            it.trackTotalHits { trackHits ->
                trackHits.enabled(true)
            }
            it.query { query ->
                query.bool { boolQuery ->
                    boolQuery.filter { filter ->
                        filter.terms { termsFilter ->
                            termsFilter.field("${ImageMapping::referencingDocument.name}.keyword")
                            termsFilter.terms { terms ->
                                terms.value(referenceDocuments)
                            }
                        }
                    }
                }
            }
        }, ImageMapping::class.java)

        return SummitSearchGetThumbnailsResponse(
            hits = result.hits().hits().filterNot {
                it.source() == null && it.source()?.referencingDocument != null
            }.map {
                SummitSearchImage(
                    referencingDocument = it.source()!!.referencingDocument,
                    source = it.source()!!.source,
                    dataStoreReference = it.source()!!.dataStoreReference,
                    description = it.source()!!.description
                )
            }.groupBy {
                it.referencingDocument!!
            }
        )
    }

    fun indexImage(request: SummitSearchImagePutRequest) {
        indexImage(ImageType.STANDARD, request)
    }

    fun indexThumbnail(request: SummitSearchImagePutRequest) {
        indexImage(ImageType.THUMBNAIL, request)
    }

    private fun indexImage(type: ImageType, request: SummitSearchImagePutRequest) {
        log.info { "Indexing image: $type for source: ${request.source}" }

        val result = elasticSearchClient.index(IndexRequest.of {
            it.index(indexName)
            it.id(generateIdFromUrl(request.source))
            it.document(ImageMapping(
                source = request.source.toString(),
                dataStoreReference = request.dataStoreReference,
                description = request.description,
                referencingDocument = request.referencingDocument?.let { url ->
                    generateIdFromUrl(url)
                },
                type = type
            ))
        })

        log.info { "Saved image: ${request.source}: ${result.result()}" }
    }


    fun createIfNotExists() {
        if (!elasticSearchClient.indexExists(indexName)) {
            log.info { "Index: $indexName not found. Creating now." }
            elasticSearchClient.indices().create(CreateIndexRequest.of{
                it.index(indexName)
            })
        }
    }


    companion object {
        const val INDEX_NAME = "summit-search-images"
    }
}


internal data class ImageMapping(
    val source: String,
    val dataStoreReference: String,
    val description: String,
    val referencingDocument: String?,
    val type: ImageType
)

internal enum class ImageType {
    THUMBNAIL,
    STANDARD
}

data class SummitSearchImagePutRequest(
    val source: URL,
    val dataStoreReference: String,
    val description: String,
    val referencingDocument: URL?
)

data class SummitSearchGetThumbnailsRequest(
    val referenceDocuments: Set<URL>
)

data class SummitSearchGetThumbnailsResponse(
    private val hits: Map<String, List<SummitSearchImage>>
) {
    fun getThumbnailsByUrl(url: URL): List<SummitSearchImage>? {
        return hits[generateIdFromUrl(url)]
    }
}

data class SummitSearchImage(
    val dataStoreReference: String,
    val description: String,
    val source: String,
    val referencingDocument: String?
)