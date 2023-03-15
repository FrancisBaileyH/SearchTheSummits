package com.francisbailey.summitsearch.index.coordinator.worker

import org.springframework.stereotype.Service

@Service
class WorkerHealthTracker {
    fun getHealthyWorkers(): List<Worker> {
        return emptyList()
    }
}