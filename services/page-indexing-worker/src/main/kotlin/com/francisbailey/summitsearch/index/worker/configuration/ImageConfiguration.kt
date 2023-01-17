package com.francisbailey.summitsearch.index.worker.configuration

import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.PngWriter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ImageConfiguration {

    @Bean
    open fun imageLoader() = ImmutableImageLoader.create()

    @Bean
    open fun imageWriter() = PngWriter()
}