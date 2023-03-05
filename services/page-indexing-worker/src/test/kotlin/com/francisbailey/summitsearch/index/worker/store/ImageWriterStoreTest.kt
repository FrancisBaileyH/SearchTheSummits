package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.services.common.RegionConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URL

class ImageWriterStoreTest {

    private val storageClient = mock<S3Client>()

    private val storeName = "TestThumbnailStore"

    private val endpoint = URL("https://some-endpoint.com")

    private val writerStore = ImageWriterStore(storageClient, storeName, endpoint, RegionConfig.EnvironmentType.DEV)

    @Test
    fun `generates expected id from URL`() {
        val url = URL("https://www.francisbaileyh.com/some/path/!with/illegal/chars.png")
        val expectedId = "dev-thumbnails/e7ce5206e216b1e5717cce5adc27ceaef939c568/534061934380f24125de1131dade1bbb6e9b5086.jpg"

        assertEquals(expectedId, ImageWriterStore.buildPathFromUrl("dev-", url, ImageStoreType.THUMBNAIL))
    }

    @Test
    fun `storeExists returns false if NoSuchBucketFoundException is thrown`() {
        whenever(storageClient.headBucket(any<HeadBucketRequest>())).thenThrow(NoSuchBucketException.builder().build())

        assertFalse(writerStore.storeExists())

        verify(storageClient).headBucket(org.mockito.kotlin.check<HeadBucketRequest> {
            assertEquals(it.bucket(), storeName)
        })
    }

    @Test
    fun `storeExists returns true if NoSuchBucketFoundException is not thrown`() {
        assertTrue(writerStore.storeExists())

        verify(storageClient).headBucket(org.mockito.kotlin.check<HeadBucketRequest> {
            assertEquals(it.bucket(), storeName)
        })
    }

    @Test
    fun `exists returns false if NoSuchKeyException is thrown`() {
        val url = URL("https://www.somewhere.com/image.png")
        whenever(storageClient.headObject(any<HeadObjectRequest>())).thenThrow(NoSuchKeyException.builder().build())
        assertFalse(writerStore.exists(url, ImageStoreType.STANDARD))

        verify(storageClient).headObject(org.mockito.kotlin.check<HeadObjectRequest> {
            assertEquals(it.bucket(), storeName)
            assertEquals(it.key(), "dev-preview/203050d8f0d1cc842097714456125122a57eb61a/15bb72369ef45318f3556c9cd563aa393f1216d9.jpg")
        })
    }

    @Test
    fun `exists returns true if NoSuchKeyException is not thrown`() {
        val url = URL("https://www.somewhere.com/image.png")

        assertTrue(writerStore.exists(url, ImageStoreType.STANDARD))

        verify(storageClient).headObject(org.mockito.kotlin.check<HeadObjectRequest> {
            assertEquals(it.bucket(), storeName)
            assertEquals(it.key(), "dev-preview/203050d8f0d1cc842097714456125122a57eb61a/15bb72369ef45318f3556c9cd563aa393f1216d9.jpg")
        })
    }

    @Test
    fun `creates bucket if it does not exist`() {
        whenever(storageClient.headBucket(any<HeadBucketRequest>())).thenThrow(NoSuchBucketException.builder().build())

        writerStore.createStoreIfNotExists()

        verify(storageClient).createBucket(org.mockito.kotlin.check<CreateBucketRequest> {
            assertEquals(storeName, it.bucket())
        })
    }

    @Test
    fun `saves object to store`() {
        val path = "some-path"
        val reference = writerStore.save(path, ByteArray(1))

        assertEquals("https://TestThumbnailStore.some-endpoint.com/some-path", reference.toString())
        verify(storageClient).putObject(org.mockito.kotlin.check<PutObjectRequest> {
            assertEquals(storeName, it.bucket())
            assertEquals(path, it.key())
            assertEquals(ObjectCannedACL.PUBLIC_READ, it.acl())
        }, any<RequestBody>())
    }

    @Test
    fun `saves object to store with expected URL`() {
        val expectedPath = "dev-thumbnails/d2c789dbed3ca341e03395189ba64b35c9732e8d/1c682072958afb17941bb1754e5f8b7fd2941f3d.jpg"
        val source = URL("https://francisbaileyh.com/test/image.png")
        val reference = writerStore.save(source, ByteArray(1), ImageStoreType.THUMBNAIL)

        assertEquals("https://TestThumbnailStore.some-endpoint.com/$expectedPath", reference.toString())
        verify(storageClient).putObject(org.mockito.kotlin.check<PutObjectRequest> {
            assertEquals(storeName, it.bucket())
            assertEquals(expectedPath, it.key())
            assertEquals(ObjectCannedACL.PUBLIC_READ, it.acl())
        }, any<RequestBody>())
    }

    @Test
    fun `resolve returns full endpoint from path`() {
        val testPath = "www-francisbailey-com/abc123.png"

        val url = writerStore.resolve(testPath)

        assertEquals("https://$storeName.${endpoint.host}/$testPath", url.toString())
    }
}