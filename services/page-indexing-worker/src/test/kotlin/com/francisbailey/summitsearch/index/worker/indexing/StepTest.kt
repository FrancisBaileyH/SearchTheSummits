package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.net.URL
import java.time.Duration
import java.util.*

open class StepTest {

    protected val perQueuecircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }

    protected val depencencyCircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }

    protected val monitor = PipelineMonitor(
        sourceCircuitBreaker = perQueuecircuitBreaker,
        dependencyCircuitBreaker = depencencyCircuitBreaker,
        meter = SimpleMeterRegistry()
    )

    protected val defaultIndexTask = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.HTML,
            refreshIntervalSeconds = Duration.ofMinutes(60).seconds
        )
    )


}