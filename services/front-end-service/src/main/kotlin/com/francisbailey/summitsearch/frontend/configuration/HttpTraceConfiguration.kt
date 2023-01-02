package com.francisbailey.summitsearch.frontend.configuration

import org.springframework.boot.actuate.trace.http.HttpTraceRepository
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class HttpTraceConfiguration {


    @Bean
    open fun httpTraceRepository(): HttpTraceRepository {
        return InMemoryHttpTraceRepository()
    }

}