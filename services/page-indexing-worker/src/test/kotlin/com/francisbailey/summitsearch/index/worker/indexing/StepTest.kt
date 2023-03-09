package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.io.File
import java.net.URL
import java.time.Duration
import java.util.*

open class StepTest {

    class TestMeterRegistry: SimpleMeterRegistry() {
        override fun timer(name: String, vararg tags: String?): Timer {
            return super.timer(name)
        }

        override fun counter(name: String, vararg tags: String?): Counter {
            return super.counter(name)
        }
    }

    protected val perQueuecircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }

    protected val depencencyCircuitBreaker = mock<CircuitBreaker> {
        on(mock.executeCallable<Unit>(any())).thenCallRealMethod()
    }

    protected val monitor = PipelineMonitor(
        sourceCircuitBreaker = perQueuecircuitBreaker,
        dependencyCircuitBreaker = depencencyCircuitBreaker,
        meter = TestMeterRegistry()
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

    private val resources = File("src/test/resources")

    fun loadHtml(name: String): Document {
        return Jsoup.parse(resources.resolve(name).readText())
    }

}