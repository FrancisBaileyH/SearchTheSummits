package com.francisbailey.summitsearch.indexservice

import co.elastic.clients.elasticsearch.indices.RefreshRequest
import com.francisbailey.summitsearch.indexservice.extension.indexExists
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL

class ImageIndexServiceTest {

    private val testServer = ElasticSearchTestServer.global()

    private val client = testServer.client()

    @Test
    fun `fetches images by referencing document`() {
        val index = "image-test"
        val service = ImageIndexService(client, 20, index).also {
            it.createIfNotExists()
        }

        val referencingUrl = URL("https://example.com")

        service.indexThumbnail(SummitSearchImagePutRequest(
            source = URL("https://example.com/image1.jpeg"),
            referencingDocument = referencingUrl,
            dataStoreReference = "some-reference",
            description = "Example description"
        ))
        service.indexThumbnail(SummitSearchImagePutRequest(
            source = URL("https://example.com/image2.jpeg"),
            referencingDocument = referencingUrl,
            dataStoreReference = "some-reference",
            description = "Example description"
        ))
        service.indexThumbnail(SummitSearchImagePutRequest(
            source = URL("https://example.com/image3.jpeg"),
            referencingDocument = URL("https://example.com/test/"),
            dataStoreReference = "some-reference",
            description = "Example description"
        ))

        client.indices().refresh(RefreshRequest.of {
            it.index(index)
        })

        val thumbnails = service.fetchThumbnails(SummitSearchGetThumbnailsRequest(setOf(
            referencingUrl
        )))

        val thumbnailHits = thumbnails.getThumbnailsByUrl(referencingUrl)

        assertEquals(2, thumbnailHits?.size)

        val expectedSources = setOf(
            "https://example.com/image2.jpeg",
            "https://example.com/image1.jpeg"
        )

        assertTrue(thumbnailHits!!.all {
            expectedSources.contains(it.source)
        })
    }


    @Test
    fun `creates index with html analyzer if index does not exist yet`() {
        val index = "create-image-index-not-exist"
        val testIndexService = ImageIndexService(client, 10, index)
        Assertions.assertFalse(client.indexExists(index))

        testIndexService.createIfNotExists()

        assertTrue(client.indexExists(index))
    }

}