package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
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
    private val environment: Environment
) {

    @Bean
    open fun imageLoader(): ImmutableImageLoader = ImmutableImageLoader.create()

    @Bean
    open fun imageWriter(): ImageWriter = JpegWriter.Default

    @Bean
    open fun imageWriterStore() = ImageWriterStore(
        s3Client,
        environment.getRequiredProperty("S3_STORE_NAME"),
        URL(environment.getRequiredProperty("S3_ENDPOINT"))
    )

    val imageGenerationAllowList = setOf(
        "www.francisbaileyh.com",
        "stevensong.com",
        "alpinebaking.com",
        "www.desertmountaineer.com",
        "www.lemkeclimbs.com",
        "www.countryhighpoints.com",
        "besthikesbc.ca",
        "www.drdirtbag.com",
        "climberkyle.com",
        "havetent.com",
        "alpinejournals.com",
        "alpinevagabonds.com",
        "www.akmountain.com",
        "colinhaley.com"
    )

}