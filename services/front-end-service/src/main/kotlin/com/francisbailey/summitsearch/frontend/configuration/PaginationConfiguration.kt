package com.francisbailey.summitsearch.frontend.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PaginationConfiguration {

    @Bean
    open fun documentResultsPerPage(): Int = 10

    @Bean
    open fun imageResultsPerPage(): Int = 30

    @Bean
    open fun previewImageResultsPerRequest(): Int = 6
}