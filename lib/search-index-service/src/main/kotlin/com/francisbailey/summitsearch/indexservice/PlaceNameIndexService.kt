package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.mapping.CompletionProperty
import co.elastic.clients.elasticsearch._types.mapping.DateProperty
import co.elastic.clients.elasticsearch._types.mapping.GeoPointProperty
import co.elastic.clients.elasticsearch._types.mapping.Property
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.search.CompletionSuggester
import co.elastic.clients.elasticsearch.core.search.FieldSuggester
import co.elastic.clients.elasticsearch.core.search.Suggester
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import com.francisbailey.summitsearch.indexservice.common.toSha1
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import mu.KotlinLogging
import java.time.Clock

class PlaceNameIndexService(
    val indexName: String,
    private val client: ElasticsearchClient,
    private val synonyms: List<String> = emptyList(),
    private val clock: Clock = Clock.systemUTC()
) {

    private val log = KotlinLogging.logger { }

    fun query(request: PlaceNameQueryRequest): List<PlaceNameHit> {
        require(request.query.length <= MAX_QUERY_SIZE) {
            "Query term must not contain more than $MAX_QUERY_SIZE characters"
        }

        val results = client.search(SearchRequest.of {
            it.index(indexName)
            it.query { query ->
                query.multiMatch { match ->
                    match.query(request.query)
                    match.fields(PlaceNameMapping::name.name, PlaceNameMapping::alternativeName.name)
                    match.analyzer(ANALYZER_NAME)
                    match.type(TextQueryType.PhrasePrefix)
                }
            }
        }, PlaceNameMapping::class.java)

        return results.hits().hits().map {
            val source = it.source()!!
            PlaceNameHit(
                name = source.name,
                elevation = source.elevation,
                description = source.description,
                alternativeName = source.alternativeName,
                source = source.source,
                latitude = source.location.lat,
                longitude = source.location.lon
            )
        }
    }

    /**
     * Return a suggestion + display name whenever a suggestion is found.
     *
     * The display name is the combination of the normal name + alternative name
     * while the suggestion is the one of the alt name or normal name (whichever you started typing in)
     *
     * E.g. For Quirin Peak which has alternative name of Hydro Mountain if you start typing "Hyd"
     * You would get:
     *
     * display name = Quirin Peak (Hydro Mountain)
     * suggestion = Hydro Peak
     *
     * vice versa
     *
     * Search = "quir"
     *
     * display name = "Quirin Peak (Hydro Mountain)
     * suggestion = "Quirin Peak"
     */
    fun autoCompleteQuery(request: AutoCompleteQueryRequest): List<PlaceNameSuggestion> {
        require(request.prefix.length <= MAX_AUTO_COMPLETE_QUERY_SIZE) {
            "Query term must not contain more than $MAX_AUTO_COMPLETE_QUERY_SIZE characters"
        }

        val results = client.search(SearchRequest.of {
            it.index(indexName)
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
                val alternativeName = it.source()?.alternativeName
                val altSuggestion = alternativeName?.let { alt -> " ($alt)" } ?: ""
                val name = it.source()!!.name

                val suggestion = if (alternativeName != null && !name.contains(it.text())) {
                    alternativeName
                } else {
                    name
                }

                PlaceNameSuggestion(
                    suggestion = suggestion,
                    displayName = it.source()!!.name + altSuggestion
                )
            } ?: emptyList()
    }

    fun index(request: PlaceNameIndexRequest) {
        index(listOf(request))
    }

    fun index(requests: List<PlaceNameIndexRequest>) {
        client.bulk {
            it.index(indexName)
            it.operations(requests.map { request ->
                BulkOperation.of { operation ->
                    operation.index { indexOperation ->
                        indexOperation.id(generateId(request.name, request.latitude, request.longitude))
                        indexOperation.document(
                            PlaceNameMapping(
                                name = request.name,
                                alternativeName = request.alternativeName,
                                nameSuggester = generateSuggestions(request.name) + generateSuggestions(request.alternativeName),
                                description = request.description,
                                source = request.source,
                                elevation = request.elevation,
                                location = PlaceNameLocationMapping(
                                    lat = request.latitude,
                                    lon = request.longitude
                                ),
                                lastUpdateTime = clock.millis()
                            )
                        )
                    }
                }
            })
        }
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

    internal fun generateId(name: String, latitude: Double, longitude: Double): String  {
        return "${name.lowercase()}-${latitude}-${longitude}".toSha1()
    }

    /**
     * Takes a name like "Mount Judge Howay" and produces a list like:
     * - Howay
     * - Judge Howay
     * - Mount Judge Howay
     *
     * This enables someone to search the prefix: "How" and get "Mount Judge Howay"
     */
    internal fun generateSuggestions(name: String?): List<String> {
        return name?.let {
            it.split(" ")
                .reversed()
                .runningReduce { acc, s ->
                    "$s $acc"
                }
        } ?: emptyList()
    }

    companion object {
        const val ANALYZER_NAME = "standard_with_synonyms"
        const val SYNONYM_FILTER_NAME = "synonym_graph"
        const val AUTO_COMPLETE_RESULT_COUNT = 6
        const val MAX_AUTO_COMPLETE_QUERY_SIZE = 40
        const val MAX_QUERY_SIZE = 100
    }
}

data class PlaceNameMapping(
    val name: String,
    val alternativeName: String? = null,
    val nameSuggester: List<String>,
    val elevation: Int?,
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
    val alternativeName: String? = null,
    val elevation: Int?,
    val description: String? = null,
    val source: String? = null,
    val latitude: Double,
    val longitude: Double
)

data class PlaceNameHit(
    val name: String,
    val alternativeName: String? = null,
    val elevation: Int?,
    val description: String? = null,
    val source: String? = null,
    val latitude: Double,
    val longitude: Double
)

data class PlaceNameSuggestion(
    val displayName: String,
    val suggestion: String
)