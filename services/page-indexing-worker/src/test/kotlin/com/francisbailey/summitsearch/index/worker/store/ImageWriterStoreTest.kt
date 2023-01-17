package com.francisbailey.summitsearch.index.worker.store

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.ObjectCannedACL
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URL

class ImageWriterStoreTest {

    private val storageClient = mock<S3Client>()

    private val storeName = "TestThumbnailStore"

    private val writerStore = ImageWriterStore(storageClient, storeName)

    @Test
    fun `generates expected id from URL`() {
        val url = URL("https://www.francisbaileyh.com/some/path/!with/illegal/chars.png")
        val expectedId = "www-francisbaileyh-com/some-path-with-illegal-chars.png"

        assertEquals(expectedId, ImageWriterStore.buildPathFromUrl(url))
    }

    @Test
    fun `exists returns false if NoSuchBucketFoundException is thrown`() {
        whenever(storageClient.headBucket(any<HeadBucketRequest>())).thenThrow(NoSuchBucketException.builder().build())

        assertFalse(writerStore.exists())

        verify(storageClient).headBucket(org.mockito.kotlin.check<HeadBucketRequest> {
            assertEquals(it.bucket(), storeName)
        })
    }

    @Test
    fun `exists returns true if NoSuchBucketFoundException is not thrown`() {
        assertTrue(writerStore.exists())

        verify(storageClient).headBucket(org.mockito.kotlin.check<HeadBucketRequest> {
            assertEquals(it.bucket(), storeName)
        })
    }

    @Test
    fun `creates bucket if it does not exist`() {
        whenever(storageClient.headBucket(any<HeadBucketRequest>())).thenThrow(NoSuchBucketException.builder().build())

        writerStore.createIfNotExists()

        verify(storageClient).createBucket(org.mockito.kotlin.check<CreateBucketRequest> {
            assertEquals(storeName, it.bucket())
        })
    }

    @Test
    fun `saves object to store`() {
        val path = "some-path"
        writerStore.save(path, ByteArray(1))

        verify(storageClient).putObject(org.mockito.kotlin.check<PutObjectRequest> {
            assertEquals(storeName, it.bucket())
            assertEquals(path, it.key())
            assertEquals(ObjectCannedACL.PUBLIC_READ, it.acl())
        }, any<RequestBody>())
    }
}