package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.TransportUtils
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient

class SummitSearchIndexServiceFactory {

    companion object {

        private val clientCache = mutableMapOf<String, ElasticsearchClient>()

        fun buildQueryStatsIndex(configuration: SearchIndexServiceConfiguration, useClientCache: Boolean = true): QueryStatsIndex {
            return QueryStatsIndex(buildESClient(configuration))
        }

        fun buildImageIndex(configuration: SearchIndexServiceConfiguration, useClientCache: Boolean = true): ImageIndexService {
            return ImageIndexService(buildESClient(configuration), configuration.paginationResultSize)
        }

        fun build(configuration: SearchIndexServiceConfiguration, useClientCache: Boolean = true): SummitSearchIndexService {
            return SummitSearchIndexService(buildESClient(configuration), configuration.paginationResultSize)
        }

        private fun buildESClient(configuration: SearchIndexServiceConfiguration, useClientCache: Boolean = true): ElasticsearchClient {
            val cachedClient = clientCache[configuration.connectionIdentifier()]

            if (useClientCache && cachedClient != null) {
                return cachedClient
            }

            val restClientBuilder = RestClient.builder(
                HttpHost(
                    configuration.endpoint,
                    configuration.port,
                    configuration.scheme
                )
            )

            if (configuration.scheme == "https") {
                val sslContext = TransportUtils.sslContextFromCaFingerprint(configuration.fingerprint)
                val credentialsProvider = BasicCredentialsProvider().apply {
                    this.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(
                        configuration.username,
                        configuration.password
                    ))
                }

                restClientBuilder.setHttpClientConfigCallback {
                    it.setSSLContext(sslContext)
                    it.setDefaultCredentialsProvider(credentialsProvider)
                }
            }

            val client = ElasticsearchClient(
                RestClientTransport(
                    restClientBuilder.build(),
                    JacksonJsonpMapper().apply {
                        this.objectMapper().registerModule(KotlinModule())
                    }
                )
            )

            if (useClientCache) {
                clientCache[configuration.connectionIdentifier()] = client
            }

            return client
        }
    }
}


data class SearchIndexServiceConfiguration(
    val fingerprint: String,
    val username: String,
    val password: String,
    val endpoint: String,
    val scheme: String = "https",
    val port: Int = 9200,
    val paginationResultSize: Int = 20
) {
    fun connectionIdentifier(): String = "$username@$password:$scheme://$endpoint:$port"
}