package com.francisbailey.summitsearch.index.worker.store

import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URL


class ImageWriterStore(
    private val storageClient: S3Client,
    private val storeName: String
) {
    private val log = KotlinLogging.logger { }

    fun save(path: String, imageData: ByteArray) {
        log.info { "Saving image with path: $path to store $storeName" }

        storageClient.putObject(PutObjectRequest.builder()
            .bucket(storeName)
            .key(path)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .build(),
            RequestBody.fromBytes(imageData)
        )
    }

    fun exists(): Boolean {
        return try {
            storageClient.headBucket(HeadBucketRequest.builder()
                .bucket(storeName)
                .build()
            )
            true
        } catch (e: NoSuchBucketException) {
            false
        }
    }

    fun createIfNotExists() {
        log.info { "Checking if store: $storeName exists" }
        if (!exists()) {
            log.info { "Store not found. Creating store now." }
            storageClient.createBucket(CreateBucketRequest.builder()
                .bucket(storeName)
                .build()
            )
        }
    }

    /**
     * @TODO generate UUID with .png extension
     */
    companion object {
        private val CHARACTER_FILTER = Regex("[^a-zA-Z0-9-/.]")

        fun buildPathFromUrl(url: URL): String {
            val basePath = url.host.replace(".", "-")
            val name = url.path.substringAfter("/").replace("/", "-")

            return "$basePath/$name".replace(CHARACTER_FILTER, "")
        }
    }
}