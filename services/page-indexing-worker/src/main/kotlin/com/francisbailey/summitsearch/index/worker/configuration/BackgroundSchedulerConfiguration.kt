package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.task.PageIndexingTaskCoordinator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled


@Configuration
open class BackgroundSchedulerConfiguration(
    private val pageIndexingTaskCoordinator: PageIndexingTaskCoordinator
) {

    @Scheduled(fixedRate = 1000)
    open fun runPageIndexingCoordinator() {
        pageIndexingTaskCoordinator.coordinateTaskExecution()
    }

}