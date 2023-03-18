package com.francisbailey.summitsearch.index.coordinator.configuration

import com.francisbailey.summitsearch.index.coordinator.worker.Worker
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class WorkerConfiguration(
    @Value("\${worker.endpoints}")
    private val workerEndpoints: String
) {

    private val maxAssignmentsPerWorker = 3

    @Bean
    open fun workers(): Set<Worker> {
        return workerEndpoints.split(",").map {
            Worker(
                endpoint = it,
                availableSlots = maxAssignmentsPerWorker
            )
        }.toSet()
    }
}