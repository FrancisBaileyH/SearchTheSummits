package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.task.DefaultRateLimiter
import com.francisbailey.summitsearch.index.worker.task.RateLimiter
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Configuration
open class TaskConfiguration {

    @Bean
    open fun indexingTaskExecutor(): Executor {
        return Executors.newFixedThreadPool(INDEX_TASK_THREAD_COUNT)
    }

    @Bean
    open fun indexingTaskRateLimiter(): RateLimiter<String> {
        return DefaultRateLimiter(
            bucketBuilder = Bucket
                .builder()
                .addLimit(Bandwidth.simple(1, INDEX_TASK_INTERVAL))
        )
    }

    companion object {
        const val INDEX_TASK_THREAD_COUNT = 100
        val INDEX_TASK_INTERVAL: Duration = Duration.ofSeconds(1)
    }

}