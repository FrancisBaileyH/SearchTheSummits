package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.extension.toSha1
import com.sun.scenario.effect.ImageData
import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URL


class ImageWriterStore(
    private val storageClient: S3Client,
    private val storeName: String,
    private val endpoint: URL
) {
    private val log = KotlinLogging.logger { }

    fun save(source: URL, imageData: ByteArray): URL {
        return save(buildPathFromUrl(source), imageData)
    }

    fun save(path: String, imageData: ByteArray): URL {
        log.info { "Saving image with path: $path to store $storeName" }

        storageClient.putObject(PutObjectRequest.builder()
            .bucket(storeName)
            .key(path)
            .acl(ObjectCannedACL.PUBLIC_READ)
            .build(),
            RequestBody.fromBytes(imageData)
        )

        return resolve(path)
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

    fun resolve(path: String): URL {
        return URL("${endpoint.protocol}://$storeName.${endpoint.host}/$path")
    }

    companion object {
        fun buildPathFromUrl(url: URL, extension: String = "png"): String {
            return "${url.host.toSha1()}/${url.path.toSha1()}.$extension"
        }
    }
}