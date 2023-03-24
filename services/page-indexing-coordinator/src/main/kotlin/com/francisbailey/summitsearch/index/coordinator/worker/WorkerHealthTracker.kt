package com.francisbailey.summitsearch.index.coordinator.worker

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit


@Service
class WorkerHealthTracker(
    private val workers: Set<Worker>,
    private val workerClient: WorkerClient
) {
    private val log = KotlinLogging.logger { }

    private val workerHealthMap = workers.associateWith {
        HeartBeatTracker()
    }.toMutableMap()

    private val healthyWorkers = mutableSetOf<Worker>()

    fun getHealthyWorkers(): Set<Worker> {
        return healthyWorkers
    }

    /**
     * Simple heart beat mechanism where in we send a heart beat
     * every second. If we see a failure, we begin to track the worker
     * and after maxFailCount is exceeded, mark the host as unhealthy
     *
     * If there's an intermittent issue, the failures can continue to climb
     * up until the host is considered failing. However, if there's a one off
     * error, the tracker will monitor for recoveries. Should the host recover
     * after recoveryThreshold, it will again be added to the healthy worker pool
     *
     * This is count based not time based, so we must carefully evaluate changes to
     * the scheduling of this function.
     */
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.SECONDS)
    fun monitorWorkers() {
        workers.forEach {
            try {
                workerClient.sendHeartBeat(it)
                log.debug { "Received heartbeat reply from: $it" }

                workerHealthMap[it]?.let { tracker ->
                    tracker.recoveries += 1

                    log.info { "Received recovery from $it. Recovery count: ${tracker.recoveries}" }

                    if (tracker.recoveries > RECOVERY_THRESHOLD) {
                        log.info { "Worker: $it, fully recovered. Re-adding to healthy worker pool" }
                        workerHealthMap.remove(it)
                        healthyWorkers.add(it)
                    }
                }
            } catch (e: Exception) {
                log.debug(e) { "Heart beat failed because: "}
                log.info { "Heart beat to: $it failed" }
                val tracker = workerHealthMap.getOrPut(it) { HeartBeatTracker() }

                tracker.recoveries = 0
                tracker.failures = MAX_TRACKING_FAILURES.coerceAtMost(tracker.failures + 1)

                if (tracker.failures >= MAX_FAIL_COUNT && healthyWorkers.contains(it)) {
                    log.info { "Max failure threshold of $MAX_FAIL_COUNT met. Removing: $it from the pool." }
                    healthyWorkers.remove(it)
                }
            }
        }
    }

    data class HeartBeatTracker(
        var failures: Int = 0,
        var recoveries: Int = 0
    )

    companion object {
        const val MAX_FAIL_COUNT = 5
        const val RECOVERY_THRESHOLD = 10
        const val MAX_TRACKING_FAILURES = MAX_FAIL_COUNT + 100 // just to prevent integer overflow
    }

}