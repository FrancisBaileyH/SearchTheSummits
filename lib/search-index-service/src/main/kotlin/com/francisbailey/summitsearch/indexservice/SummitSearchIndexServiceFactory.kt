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
        fun build(configuration: SearchIndexServiceConfiguration): SummitSearchIndexService {
            val sslContext = TransportUtils.sslContextFromCaFingerprint(configuration.fingerprint)
            val credentialsProvider = BasicCredentialsProvider().apply {
                this.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(
                    configuration.username,
                    configuration.password
                ))
            }

            val elasticSearchClient = ElasticsearchClient(
                RestClientTransport(
                    RestClient.builder(
                        HttpHost(
                            configuration.endpoint,
                            configuration.port,
                            configuration.scheme
                        )
                    ).setHttpClientConfigCallback {
                        it.setSSLContext(sslContext)
                        it.setDefaultCredentialsProvider(credentialsProvider)
                    }.build(),
                    JacksonJsonpMapper().apply {
                        this.objectMapper().registerModule(KotlinModule())
                    }
                )
            )

            return SummitSearchIndexService(elasticSearchClient, configuration.paginationResultSize)
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
)