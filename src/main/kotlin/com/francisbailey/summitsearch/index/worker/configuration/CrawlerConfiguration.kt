package com.francisbailey.summitsearch.index.worker.configuration

import org.springframework.context.annotation.Configuration
import kotlin.text.Charsets

@Configuration
open class CrawlerConfiguration {

    val charsetOverride = hashMapOf(
        "goldenscrambles.ca" to Charsets.ISO_8859_1
    )

}