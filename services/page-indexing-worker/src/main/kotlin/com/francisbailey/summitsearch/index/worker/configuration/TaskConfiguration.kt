package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.task.TaskPermitService
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
open class TaskConfiguration {

    @Bean
    open fun indexingTaskExecutor(): Executor = Executors.newFixedThreadPool(INDEX_TASK_THREAD_COUNT)

    @Bean
    open fun linkDiscoveryTaskExecutor(): Executor = Executors.newFixedThreadPool(LINK_DISCOVERY_THREAD_COUNT)

    @Bean
    open fun circuitBreakerRegistry(): CircuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()

    /**
     * Limit to 2 requests per second with at most 2 tasks in the queue at once
     */
    @Bean
    open fun taskRateLimiterRegistry(): RateLimiterRegistry = RateLimiterRegistry.of(
        RateLimiterConfig.custom()
            .limitRefreshPeriod(INDEX_TASK_INTERVAL)
            .limitForPeriod(1)
            .timeoutDuration(Duration.ofMillis(0))
            .build()
    )

    /**
     * Note with long polling and 100 threads, 2 threads can be pinned
     * to empty queue very easily. Consider polling for more messages
     * per task run or clearing assignments as soon as all tasks are done.
     */
    @Bean
    open fun taskPermitService() = TaskPermitService(permits = 2)


    companion object {
        const val INDEX_TASK_THREAD_COUNT = 100
        const val LINK_DISCOVERY_THREAD_COUNT = 10
        val INDEX_TASK_INTERVAL: Duration = Duration.ofSeconds(2)
    }

}