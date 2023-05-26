package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.CompletionProperty
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.GeoPointProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester
import co.elastic.clients.elasticsearch.core.search.FieldSuggester
import co.elastic.clients.elasticsearch.core.search.SourceConfig
import co.elastic.clients.elasticsearch.core.search.Suggester
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.common.toSha1
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import java.time.Clock

class PlaceNameIndexService(
    val indexName: String,
    private val client: ElasticsearchClient,
    private val synonyms: List<String> = emptyList(),
    private val clock: Clock = Clock.systemUTC()
) {

    fun query(request: PlaceNameQueryRequest): List<PlaceNameHit> {
        val results = client.search(SearchRequest.of {
            it.index(indexName)
            it.query { query ->
                query.matchPhrasePrefix { match ->
                    match.query(request.query)
                    match.analyzer(ANALYZER_NAME)
                    match.field(PlaceNameMapping::name.name)
                }
            }
        }, PlaceNameMapping::class.java)

        return results.hits().hits().map {
            val source = it.source()!!
            PlaceNameHit(
                name = source.name,
                elevation = source.elevation,
                description = source.description,
                source = source.source,
                latitude = source.location.lat,
                longitude = source.location.lon
            )
        }
    }

    fun autoCompleteQuery(request: AutoCompleteQueryRequest): List<PlaceNameSuggestion> {
        val results = client.search(SearchRequest.of {
            it.index(indexName)
            it.source(SourceConfig.of { sourceConfig ->
                sourceConfig.fetch(false)
            })
            it.suggest(Suggester.of { suggester ->
                suggester.suggesters(mapOf(
                    PlaceNameMapping::nameSuggester.name to FieldSuggester.of { fieldSuggester ->
                        fieldSuggester.prefix(request.prefix)
                        fieldSuggester.completion(CompletionSuggester.of { comSuggester ->
                            comSuggester.skipDuplicates(true)
                            comSuggester.field(PlaceNameMapping::nameSuggester.name)
                            comSuggester.size(AUTO_COMPLETE_RESULT_COUNT)
                        })
                    }
                ))
            })
        }, PlaceNameMapping::class.java)

        return results.suggest()[PlaceNameMapping::nameSuggester.name]
            ?.first()
            ?.completion()
            ?.options()
            ?.map {
                PlaceNameSuggestion(
                    suggestion = it.text()
                )
            } ?: emptyList()
    }

    fun index(request: PlaceNameIndexRequest) {
        client.index(IndexRequest.of {
            it.index(indexName)
            it.id("${request.name.lowercase()}-${request.latitude}-${request.longitude}".toSha1())
            it.document(PlaceNameMapping(
                name = request.name,
                nameSuggester = listOf(request.name),
                description = request.description,
                source = request.source,
                elevation = request.elevation,
                location = PlaceNameLocationMapping(
                    lat = request.latitude,
                    lon = request.longitude
                ),
                lastUpdateTime = clock.millis()
            ))
        })
    }

    fun createIfNotExists() {
        if (!client.indexExists(indexName)) {
            client.indices().create(CreateIndexRequest.of{
                it.index(indexName)
                it.mappings { mapping ->
                    mapping.properties(mapOf(
                        PlaceNameMapping::nameSuggester.name to Property.of { property ->
                            property.completion(CompletionProperty.Builder().build())
                        },
                        PlaceNameMapping::lastUpdateTime.name to Property.of { property ->
                            property.date(DateProperty.Builder().build())
                        },
                        PlaceNameMapping::location.name to Property.of { property ->
                            property.geoPoint(GeoPointProperty.Builder().build())
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

    companion object {
        const val ANALYZER_NAME = "standard_with_synonyms"
        const val SYNONYM_FILTER_NAME = "synonym_graph"
        const val AUTO_COMPLETE_RESULT_COUNT = 6
    }
}

data class PlaceNameMapping(
    val name: String,
    val nameSuggester: List<String>,
    val elevation: Int,
    val description: String?,
    val source: String?,
    val location: PlaceNameLocationMapping,
    val lastUpdateTime: Long
)

data class PlaceNameLocationMapping(
    val lat: Double,
    val lon: Double
)

data class AutoCompleteQueryRequest(
    val prefix: String
)

data class PlaceNameQueryRequest(
    val query: String
)

data class PlaceNameIndexRequest(
    val name: String,
    val elevation: Int,
    val description: String? = null,
    val source: String? = null,
    val latitude: Double,
    val longitude: Double
)

data class PlaceNameHit(
    val name: String,
    val elevation: Int,
    val description: String? = null,
    val source: String? = null,
    val latitude: Double,
    val longitude: Double
)

data class PlaceNameSuggestion(
    val suggestion: String
)