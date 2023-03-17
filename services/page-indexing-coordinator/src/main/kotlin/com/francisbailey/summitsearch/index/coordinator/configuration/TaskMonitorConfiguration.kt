package com.francisbailey.summitsearch.index.coordinator.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class TaskMonitorConfiguration {

    @Bean
    open fun emptyQueueMonitorDuration(): Duration = Duration.ofMinutes(30)
}