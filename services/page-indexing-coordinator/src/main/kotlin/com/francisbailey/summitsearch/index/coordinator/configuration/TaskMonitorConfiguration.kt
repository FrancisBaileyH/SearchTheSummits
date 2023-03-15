package com.francisbailey.summitsearch.index.coordinator.configuration

import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class TaskMonitorConfiguration {
    val emptyQueueMonitorDuration = Duration.ofMinutes(30)
}