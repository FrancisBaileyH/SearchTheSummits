package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jsonb.JsonbJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

class ElasticSearchTestServer {

    private lateinit var container: ElasticsearchContainer
    private lateinit var client: ElasticsearchClient

    fun start(): ElasticSearchTestServer {
        val password = "changeme"
        val elasticSearchImage = "docker.elastic.co/elasticsearch/elasticsearch:8.5.1"
        val image = DockerImageName.parse(elasticSearchImage)

        container = ElasticsearchContainer(image)
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m")
            .withEnv("path.repo", "/tmp") // for snapshots
            .withStartupTimeout(Duration.ofSeconds(30))
            .withPassword(password)

        container.start()

        val port = container.getMappedPort(9200)
        val host = HttpHost("localhost", port, "https")

        val sslContext = container.createSslContextFromCa()
        val credentialsProvider = BasicCredentialsProvider().apply {
            this.setCredentials(AuthScope.ANY, UsernamePasswordCredentials("elastic", password))
        }

        val restClient = RestClient.builder(host)
            .setHttpClientConfigCallback {
                it.setDefaultCredentialsProvider(credentialsProvider)
                it.setSSLContext(sslContext)
            }
            .build()

        val transport = RestClientTransport(restClient, JsonbJsonpMapper())

        client = ElasticsearchClient(transport)

        return this
    }

    fun client(): ElasticsearchClient = client


    companion object {
        var global: ElasticSearchTestServer? = null

        fun global(): ElasticSearchTestServer {

            if (global == null) {
                global = ElasticSearchTestServer().start()
            }

            return global!!
        }
    }

}