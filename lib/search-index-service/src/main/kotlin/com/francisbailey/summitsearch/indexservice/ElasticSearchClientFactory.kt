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

class ElasticSearchClientFactory {

    data class Configuration(
        val endpoint: String,
        val port: Int = 9200,
        val scheme: String = "https",
        val username: String,
        val password: String,
        val fingerprint: String
    )

    companion object {
        fun build(configuration: Configuration): ElasticsearchClient {
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
                    this.setCredentials(
                        AuthScope.ANY, UsernamePasswordCredentials(
                        configuration.username,
                        configuration.password
                    ))
                }

                restClientBuilder.setHttpClientConfigCallback {
                    it.setSSLContext(sslContext)
                    it.setDefaultCredentialsProvider(credentialsProvider)
                }
            }

            return ElasticsearchClient(
                RestClientTransport(
                    restClientBuilder.build(),
                    JacksonJsonpMapper().apply {
                        this.objectMapper().registerModule(KotlinModule.Builder().build())
                    }
                )
            )
        }
    }
}