package com.francisbailey.summitsearch.frontend.configuration

import org.springframework.boot.actuate.trace.http.HttpTraceRepository
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.AbstractRequestLoggingFilter
import javax.servlet.http.HttpServletRequest

@Configuration
open class HttpTraceConfiguration {

    @Bean
    open fun httpTraceRepository(): HttpTraceRepository {
        return InMemoryHttpTraceRepository()
    }

    @Bean
    open fun logFilter(): APIRequestLoggingFilter {
        return APIRequestLoggingFilter().apply {
            setIncludeHeaders(true)
            setBeforeMessagePrefix("API Request: ")
            setBeforeMessageSuffix("")
        }
    }

}


class APIRequestLoggingFilter: AbstractRequestLoggingFilter() {
    override fun beforeRequest(request: HttpServletRequest, message: String) {
        logger.info(message)
    }

    override fun afterRequest(request: HttpServletRequest, message: String) {
        // do not log after request
    }

    override fun shouldLog(request: HttpServletRequest): Boolean {
        return request.requestURI.startsWith("/api")
    }
}