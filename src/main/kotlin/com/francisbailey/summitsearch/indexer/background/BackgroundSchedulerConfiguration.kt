package com.francisbailey.summitsearch.indexer.background

import com.francisbailey.summitsearch.indexer.IndexingQueueProvider
import com.francisbailey.summitsearch.indexer.task.PageIndexingCoordinator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled


@Configuration
open class BackgroundSchedulerConfiguration(
    private val indexingQueueProvider: IndexingQueueProvider,
    private val pageIndexingCoordinator: PageIndexingCoordinator
) {

    @Scheduled(fixedRate = 100)
    fun runPageIndexingCoordinator() {
        pageIndexingCoordinator.coordinateTaskExecution()
    }

    @Scheduled(fixedRate = 10000)
    fun runIndexingQueueProvider() {
        indexingQueueProvider.refreshQueues()
    }

}