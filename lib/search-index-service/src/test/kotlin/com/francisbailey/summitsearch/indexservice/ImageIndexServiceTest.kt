package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.core.GetRequest
import co.elastic.clients.elasticsearch.core.GetResponse
import co.elastic.clients.elasticsearch.indices.RefreshRequest
import com.francisbailey.summitsearch.indexservice.extension.generateIdFromUrl
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URL

class ImageIndexServiceTest {

    private val testServer = ElasticSearchTestServer.global()

    private val client = testServer.client()

    private val getDocument: (URL, String) -> GetResponse<ImageMapping> = { url, index ->
        client.get(GetRequest.of {
            it.index(index)
            it.id(generateIdFromUrl(url))
        }, ImageMapping::class.java)
    }


    @Test
    fun `indexes image with expected content`() {
        val request = SummitSearchImagePutRequest(
            source = URL("https://francisbaileyh.com/wp-content/image.jpeg"),
            referencingDocumentDate = 123456789,
            referencingDocument = URL("https://francisbaileyh.com/some-page"),
            description = "This is a mountain!",
            dataStoreReference = "https://some-reference-here"
        )

        val index = "create-image-index"
        val testIndexService = ImageIndexService(client, 10, index).also {
            it.createIfNotExists()
        }

        assertFalse(getDocument(request.source, index).found())

        testIndexService.indexImage(request)

        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = getDocument(request.source, index)

        assertEquals(request.description, result.source()!!.description)
        assertEquals(request.dataStoreReference, result.source()!!.dataStoreReference)
        assertEquals(request.referencingDocumentDate, result.source()!!.referencingDocumentDate)
        assertEquals(request.referencingDocument.host, result.source()!!.referencingDocumentHost)
        assertEquals(generateIdFromUrl(request.source), result.id())
    }

    @Test
    fun `cleans malicious text from description`() {
        val request = SummitSearchImagePutRequest(
            source = URL("https://francisbaileyh.com/wp-content/image.jpeg"),
            referencingDocumentDate = 123456789,
            referencingDocument = URL("https://francisbaileyh.com/some-page"),
            description = "This is a mountain!<script>alert(something);</script><p>Hello?</p>",
            dataStoreReference = "https://some-reference-here"
        )

        val index = "create-image-index-bad-text"
        val testIndexService = ImageIndexService(client, 10, index).also {
            it.createIfNotExists()
        }

        assertFalse(getDocument(request.source, index).found())

        testIndexService.indexImage(request)

        client.indices().refresh(RefreshRequest.of { it.index(index) })

        val result = getDocument(request.source, index)

        assertEquals("This is a mountain!Hello?", result.source()!!.description)
    }


    @Test
    fun `creates index with html analyzer if index does not exist yet`() {
        val index = "create-image-index-not-exist"
        val testIndexService = ImageIndexService(client, 10, index)
        assertFalse(client.indexExists(index))

        testIndexService.createIfNotExists()

        assertTrue(client.indexExists(index))
    }

}