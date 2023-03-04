package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.francisbailey.summitsearch.services.common.RegionConfig
import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.JpegWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import software.amazon.awssdk.services.s3.S3Client
import java.net.URL

@Configuration
open class ImageConfiguration(
    private val s3Client: S3Client,
    private val environment: Environment,
    private val regionConfig: RegionConfig
) {

    @Bean
    open fun imageLoader(): ImmutableImageLoader = ImmutableImageLoader.create()

    @Bean
    open fun imageWriter(): ImageWriter = JpegWriter.Default

    @Bean
    open fun imageWriterStore() = ImageWriterStore(
        storageClient = s3Client,
        storeName = environment.getRequiredProperty("S3_STORE_NAME"),
        endpoint = URL(environment.getRequiredProperty("S3_ENDPOINT")),
        domain = regionConfig.environmentType
    )
}