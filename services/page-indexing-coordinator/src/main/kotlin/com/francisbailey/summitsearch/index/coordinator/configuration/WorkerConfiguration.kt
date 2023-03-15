package com.francisbailey.summitsearch.index.coordinator.configuration

import org.springframework.context.annotation.Configuration

@Configuration
open class WorkerConfiguration {
    val maxAssignmentsPerWorker = 3
}