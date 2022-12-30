package com.francisbailey.summitsearch.index.worker.task

import com.francisbailey.summitsearch.index.worker.crawler.PageCrawlerService
import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueuePollingClient
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.concurrent.Executor


@Component
class PageIndexingTaskCoordinator(
    private val queueAssignmentStore: QueueAssignmentStore,
    private val indexingTaskExecutor: Executor,
    private val indexingTaskQueuePollingClient: IndexingTaskQueuePollingClient,
    private val summitSearchIndexService: SummitSearchIndexService,
    private val pageCrawlerService: PageCrawlerService,
    private val linkDiscoveryService: LinkDiscoveryService,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val taskRateLimiterRegistry: RateLimiterRegistry,
    private val taskPermitService: TaskPermitService,
    private val meterRegistry: MeterRegistry
) {
    private val log = KotlinLogging.logger { }

    /**
     * To crawl each site with respect to the site operator and protect down stream services we will only execute a task
     * based on the following criteria:
     *
     * 1. Per task rate limit as configured in @TaskConfiguration
     * 2. Per queue permit allowance
     *  - In the event of slow downstream calls, we can exhaust the thread pool. e.g. one or two websites
     *    taking a long time to crawl while the rest complete quickly. To ensure fair access to the pool and
     *    to prevent too many tasks for the same site executing at once, we issue a maximum number of permits.
     *    See @TaskConfiguration for permit allowance
     * 3. Per queue circuit breaker
     *   - If a particular site is experiencing issues, this circuit breaker can trip, while leaving other sites
     *     unaffected and available to crawl
     * 4. Task dependency circuit breaker
     *   - If one of the task dependencies are failing e.g. the search index, or queue service, we pause all task
     *     execution until the circuit closes again. If any of the dependencies are failing all the tasks will fail,
     *     hence the forced closure.
     */
    fun coordinateTaskExecution() {
        log.info { "Running indexing tasks now" }
        val assignments = queueAssignmentStore.getAssignments()
        val taskDependencyCircuitBreaker = circuitBreakerRegistry.circuitBreaker(DEPENDENCY_CB_KEY)
        log.info { "Running tasks against: ${assignments.size} assignments" }

        assignments.forEach { queue ->
            val perQueueCircuitBreaker = circuitBreakerRegistry.circuitBreaker(queue)

            when {
                !taskDependencyCircuitBreaker.tryAcquirePermission() -> {
                    meterRegistry.counter("$TASK_METRIC.cb.dependency.tripped").increment()
                    log.warn { "Skipping tasks because task Dependency circuit breaker has tripped" }
                }
                !perQueueCircuitBreaker.tryAcquirePermission() -> {
                    meterRegistry.counter("$TASK_METRIC.cb.queue.tripped", "queue", queue).increment()
                    log.warn { "Skipping: $queue, because per queue circuit breaker has tripped" }
                }
                else -> {
                    val permit = taskPermitService.tryAcquirePermit(queue)

                    if (permit != null) {
                        indexingTaskExecutor.execute(
                            PageIndexingTask(
                                queueName = queue,
                                pageCrawlerService = pageCrawlerService,
                                indexingTaskQueuePollingClient = indexingTaskQueuePollingClient,
                                indexService = summitSearchIndexService,
                                rateLimiterRegistry = taskRateLimiterRegistry,
                                dependencyCircuitBreaker = taskDependencyCircuitBreaker,
                                perQueueCircuitBreaker = perQueueCircuitBreaker,
                                linkDiscoveryService = linkDiscoveryService,
                                taskPermit = permit,
                                meterRegistry = meterRegistry
                            )
                        )
                    } else {
                        meterRegistry.counter("$TASK_METRIC.skipped", "queue", queue).increment()
                        log.warn { "Skipping $queue because ${taskPermitService.permits} permits have been issued already" }
                    }
                }
            }
        }
    }

    companion object {
        const val TASK_METRIC = "task.coordinating"
        const val DEPENDENCY_CB_KEY = "PageIndexingTaskDependencies"
    }
}