package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.IndexingQueueProvider
import com.francisbailey.summitsearch.index.worker.task.PageIndexingTaskCoordinator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled


@Configuration
open class BackgroundSchedulerConfiguration(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val pageIndexingTaskCoordinator: PageIndexingTaskCoordinator
) {

    @Scheduled(fixedRate = 1000)
    open fun runPageIndexingCoordinator() {
        pageIndexingTaskCoordinator.coordinateTaskExecution()
    }

    @Scheduled(fixedRate = 10000)
    open fun runIndexingQueueProvider() {
        indexingQueueProvider.refreshQueues()
    }

}