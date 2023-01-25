package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.client.*
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import java.lang.Exception


class IndexingTask(
    val queueName: String,
    private val indexingTaskQueuePollingClient: IndexingTaskQueuePollingClient,
    private val indexingPipeline: Pipeline,
    private val rateLimiterRegistry: RateLimiterRegistry,
    private val dependencyCircuitBreaker: CircuitBreaker,
    private val perQueueCircuitBreaker: CircuitBreaker,
    private val taskPermit: TaskPermit,
    private val meterRegistry: MeterRegistry
): Runnable {

    private val log = KotlinLogging.logger { }

    override fun run() = taskPermit.use {
        try {
            log.info { "Running indexing task for: $queueName" }

            if (!rateLimiterRegistry.rateLimiter(queueName).acquirePermission()) {
                meterRegistry.counter("$TASK_METRIC.rate-limited").increment()
                log.warn { "Indexing rate exceeded for: $queueName. Backing off" }
                return
            }

            val task: IndexTask? = dependencyCircuitBreaker.executeCallable {
                indexingTaskQueuePollingClient.pollTask(queueName)
            }

            if (task != null) {
                val shouldRetry = indexingPipeline.process(task, PipelineMonitor(
                    dependencyCircuitBreaker = dependencyCircuitBreaker,
                    sourceCircuitBreaker = perQueueCircuitBreaker,
                    meter = meterRegistry
                ))

                if (!shouldRetry) {
                    dependencyCircuitBreaker.executeCallable { indexingTaskQueuePollingClient.deleteTask(task) }
                } else {
                    log.info { "Returning task to queue: $queueName" }
                }
            }
        } catch (e: Exception) {
            log.error(e) { "Failed to run indexing pipeline against queue: $queueName" }
        }
    }

    companion object {
        const val TASK_METRIC = "task.indexing"
    }

}