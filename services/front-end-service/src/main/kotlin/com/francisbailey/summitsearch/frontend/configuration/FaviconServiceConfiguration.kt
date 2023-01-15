package com.francisbailey.summitsearch.frontend.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.util.*

@Configuration
open class FaviconServiceConfiguration {

    @Bean
    open fun fallbackFaviconData(): String {
        val resource = ClassPathResource("private/images/fallbackFavicon.png")
        return Base64.getEncoder().encodeToString(resource.inputStream.readBytes())
    }

}