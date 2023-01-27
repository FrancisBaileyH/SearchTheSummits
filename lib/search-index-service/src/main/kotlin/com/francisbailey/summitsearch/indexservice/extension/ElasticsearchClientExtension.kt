package com.francisbailey.summitsearch.indexservice.extension

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.JsonpDeserializer
import mu.KotlinLogging
import java.net.URL

private val log = KotlinLogging.logger { }


fun ElasticsearchClient.indexExists(indexName: String): Boolean {
    log.info { "Checking if index: $indexName exists" }
    val indexExistsResponse = this.indices().exists(ExistsRequest.of{
        it.index(indexName)
    })

    return indexExistsResponse.value().also {
        log.info { "Index found result: $it" }
    }
}


internal fun generateIdFromUrl(url: URL): String {
    val uri = url.toURI()
    val query = uri.query?.split("&")?.sorted()?.joinToString(separator = "&", prefix = "?") ?: ""
    val path = uri.path?.removeSuffix("/") ?: ""
    return "${uri.host}$path$query"
}

internal fun <T> Hit<T>.stringField(name: String): String {
    return this.fields()[name]!!.deserialize(
        JsonpDeserializer.arrayDeserializer(
            JsonpDeserializer.stringDeserializer()
        )).first()
}

internal fun <T> Hit<T>.listField(name: String): List<String>? {
    return this.fields()[name]?.deserialize(JsonpDeserializer.arrayDeserializer(
        JsonpDeserializer.stringDeserializer()
    ))
}