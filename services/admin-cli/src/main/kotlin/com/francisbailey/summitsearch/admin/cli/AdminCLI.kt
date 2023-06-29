package com.francisbailey.summitsearch.admin.cli

import com.francisbailey.summitsearch.indexservice.ElasticSearchClientFactory
import com.francisbailey.summitsearch.indexservice.PlaceNameIndexRequest
import com.francisbailey.summitsearch.indexservice.PlaceNameIndexService
import com.francisbailey.summitsearch.services.common.SummitSearchIndexes
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import de.topobyte.osm4j.core.model.iface.EntityType
import de.topobyte.osm4j.core.model.iface.OsmNode
import de.topobyte.osm4j.core.model.util.OsmModelUtil
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator
import mu.KotlinLogging

/**
 * @TODO - "peak" incorporates 5040 peak
 */
class AdminCLI: CliktCommand() {

    private val log = KotlinLogging.logger { }

    val osmFile: String by argument(help="OSM File Location")

    val endpoint by option(envvar = "ES_ENDPOINT")
    val fingerprint by option(envvar = "ES_FINGERPRINT").default("")
    val username by option(envvar = "ES_USERNAME")
    val password by option(envvar = "ES_PASSWORD")

    override fun run() {
        val client = ElasticSearchClientFactory.build(
            ElasticSearchClientFactory.Configuration(
                endpoint = endpoint!!,
                fingerprint = fingerprint,
                username = username!!,
                password = password!!,
                scheme = if (fingerprint.isBlank()) {
                    "http"
                } else {
                    "https"
                }
            ))

        val placeNameIndex = PlaceNameIndexService(
            SummitSearchIndexes.placeNameIndex,
            client,
            SummitSearchIndexes.synonyms
        ).also {
            it.createIfNotExists()
        }

        println("Reading in: $osmFile")

        val osmIterator = OsmXmlIterator(osmFile, true)

        val placeNames = osmIterator
            .filter { it.type == EntityType.Node }
            .mapNotNull {
                val node = it.entity as OsmNode
                val tags = OsmModelUtil.getTagsAsMap(node)
                val elevation = tags["ele"]?.toDoubleOrNull()
                val name = tags["name"]
                val natural = tags["natural"]
                val alternativeName = tags["alt_name"]

                if (name != null && natural in setOf("peak", "volcano")) {
                    val sanitizedNames = when {
                        name.contains("/") -> {
                            val names = name.split("/")
                            names.first() to names.last().trim()
                        }
                        name.contains("(") -> {
                            name.substringBefore("(") to alternativeName
                        }
                        name.lowercase().startsWith("cerro ") -> {
                            (name.substringAfter("Cerro") to name)
                        }
                        else -> name to alternativeName
                    }

                    PlaceNameIndexRequest(
                        name = sanitizedNames.first.trim(),
                        elevation = elevation?.toInt(),
                        latitude = node.latitude,
                        longitude = node.longitude,
                        alternativeName = sanitizedNames.second?.trim(),
                        description = tags["description"],
                        source = tags["source"]
                    )
                } else {
                    null
                }
            }

        println("Found ${placeNames.size} candidate place names. Attempting to upload now.")

        placeNames
            .chunked(BULK_INSERT_COUNT)
            .forEach {
                placeNameIndex.index(it)
                Thread.sleep(100)
            }

        println("Successfully saved all place names")
        return
    }

    companion object {
        const val BULK_INSERT_COUNT = 100
    }
}

fun main(args: Array<String>) = AdminCLI().main(args)