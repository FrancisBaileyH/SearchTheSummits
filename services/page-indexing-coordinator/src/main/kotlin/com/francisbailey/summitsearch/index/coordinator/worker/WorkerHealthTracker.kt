package com.francisbailey.summitsearch.index.coordinator.worker

import org.springframework.stereotype.Service

/**
 * On start-up
 * - After 5 successful heart beats consider host active/healthy
 *
 * During normal operation
 * - After n seconds, consider host unhealthy and do not assign it
 * - After l seconds, consider assignments available for different worker
 *
 * Scenario 1:
 * - Coordinator dies
 * 1. T-0 Worker gets no heartbeats and prepares to give up assignment
 * 2. T-4 Coordinator comes back online
 * 3. T-5 Coordinator sends heart beat
 * 4. T-5 Worker gets heart beat and considers assignments active
 * 5. T-6 Coordinator deletes assignments (ok GOOD)
 * 6. T-7 Business as usual
 */
@Service
class WorkerHealthTracker {

    fun getHealthyWorkers(): List<Worker> {
        return emptyList()
    }
}