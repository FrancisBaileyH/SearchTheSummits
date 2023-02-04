package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.store.QueueAssignmentStore
import com.francisbailey.summitsearch.index.worker.task.PageIndexingTaskCoordinator
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit


@Configuration
open class BackgroundSchedulerConfiguration(
    private val pageIndexingTaskCoordinator: PageIndexingTaskCoordinator,
    private val queueAssignmentStore: QueueAssignmentStore
) {

    @Scheduled(fixedRate = 1000)
    open fun runPageIndexingCoordinator() {
        pageIndexingTaskCoordinator.coordinateTaskExecution()
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    open fun runQueueAssignmentStoreTaskCounter() {
        queueAssignmentStore.updateTaskCount()
    }

}