package com.francisbailey.summitsearch.indexer.configuration

import com.francisbailey.summitsearch.indexer.IndexingQueueProvider
import com.francisbailey.summitsearch.indexer.task.PageIndexingCoordinator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled


@Configuration
open class BackgroundSchedulerConfiguration(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val pageIndexingCoordinator: PageIndexingCoordinator
) {

    @Scheduled(fixedRate = 1000)
    open fun runPageIndexingCoordinator() {
        pageIndexingCoordinator.coordinateTaskExecution()
    }

    @Scheduled(fixedRate = 10000)
    open fun runIndexingQueueProvider() {
        indexingQueueProvider.refreshQueues()
    }

}