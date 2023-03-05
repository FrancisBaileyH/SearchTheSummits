package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.extension.toSha1
import com.francisbailey.summitsearch.services.common.RegionConfig
import mu.KotlinLogging
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.net.URL


class ImageWriterStore(
    private val storageClient: S3Client,
    private val storeName: String,
    private val endpoint: URL,
    domain: RegionConfig.EnvironmentType
) {
    private val log = KotlinLogging.logger { }

    private val pathPrefix = when (domain) {
        RegionConfig.EnvironmentType.PROD -> ""
        else -> "${domain.name.lowercase()}-"
    }

    fun save(source: URL, imageData: ByteArray, imageStoreType: ImageStoreType): URL {
        return save(buildPathFromUrl(pathPrefix, source, imageStoreType), imageData)
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

    fun exists(source: URL, type: ImageStoreType): Boolean {
        return try {
            storageClient.headObject(HeadObjectRequest.builder()
                .bucket(storeName)
                .key(buildPathFromUrl(pathPrefix, source, type))
                .build()
            )
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }

    fun storeExists(): Boolean {
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

    fun createStoreIfNotExists() {
        log.info { "Checking if store: $storeName exists" }
        if (!storeExists()) {
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
        fun buildPathFromUrl(pathPrefix: String, url: URL, type: ImageStoreType, extension: String = "jpg"): String {
            return "$pathPrefix${type.path}/${url.host.toSha1()}/${url.path.toSha1()}.$extension"
        }
    }
}


enum class ImageStoreType(
    val path: String
) {
    THUMBNAIL("thumbnails"),
    STANDARD("preview")
}