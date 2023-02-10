package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
import mu.KLogger
import mu.KotlinLogging
import kotlin.reflect.KClass


interface ChainedRoute<T> {
    fun then(step: Step<T>): ChainedRoute<T>
    fun withHostOverride(host: String, targetStep: KClass<*>, override: Step<T>): ChainedRoute<T>
    fun finally(step: Step<T>)
}

interface ChainableRoute<T> {
    fun firstRun(step: Step<T>): ChainedRoute<T>
    fun withHostOverride(host: String, targetStep: KClass<*>, override: Step<T>): ChainedRoute<T>
}


interface Step<T> {
    fun process(entity: PipelineItem<T>, monitor: PipelineMonitor): PipelineItem<T>
    val log: KLogger
        get() = KotlinLogging.logger {  }

    val metricPrefix: String
        get() = "Pipeline.${this::class.simpleName}"
}

data class PipelineItem<T> (
    val task: IndexTask,
    var payload: T?,
    var continueProcessing: Boolean = true,
    var canRetry: Boolean = false
)

data class PipelineMonitor(
    val dependencyCircuitBreaker: CircuitBreaker,
    val sourceCircuitBreaker: CircuitBreaker,
    val meter: MeterRegistry
)

class Pipeline {
    private val routes = mutableMapOf<IndexTaskType, Route<*>>()

    fun <T> route(type: IndexTaskType, init: ChainableRoute<T>.() -> Unit): ChainableRoute<T> {
        val route = Route<T>()
        routes[type] = route
        return route.apply(init)
    }

    /**
     * @return - return true if item can be retried, false otherwise
     */
    fun process(task: IndexTask, monitor: PipelineMonitor): Boolean {
        val mappedRoute = routes[task.details.taskType]

        check(mappedRoute != null) {
            "Task type: ${task.details.taskType} has no handler in pipeline"
        }

        return mappedRoute.execute(task, monitor).canRetry
    }
}

class Route<T>: ChainedRoute<T>, ChainableRoute<T> {
    private val steps = mutableListOf<Step<T>>()

    private val log = KotlinLogging.logger { }

    private val hostOverrides: MutableMap<String, MutableMap<KClass<*>, Step<T>>> = mutableMapOf()

    private var finallyStep: Step<T>? = null

    override fun firstRun(step: Step<T>): ChainedRoute<T> {
        steps.add(step)
        return this
    }

    override fun withHostOverride(host: String, targetStep: KClass<*>, override: Step<T>): ChainedRoute<T> {
        val mappedOverride = hostOverrides.getOrPut(host) {
            mutableMapOf()
        }

        mappedOverride[targetStep] = override
        return this
    }

    /**
     * For now, an exception does not stop continuation of the pipeline. Each step
     * must explicitly call for process continuation to stop. This can be changed in
     * the future, but for now we'll leave it as is.
     */
    fun execute(task: IndexTask, monitor: PipelineMonitor): PipelineItem<T> {
        var item: PipelineItem<T> = PipelineItem(task, null)

        for (step in steps) {
            item = runStep(step, item, monitor)

            if (!item.continueProcessing) {
                monitor.meter.counter("Pipeline.aborted", "host", task.details.pageUrl.host).increment()
                monitor.meter.counter("${step.metricPrefix}.aborted", "host", task.details.pageUrl.host).increment()
                break
            }
        }

        finallyStep?.let {
            item = runStep(it, item, monitor)
        }

        return item
    }

    private fun runStep(step: Step<T>, pipelineItem: PipelineItem<T>, monitor: PipelineMonitor): PipelineItem<T> {
        val host = pipelineItem.task.details.pageUrl.host
        val stepToRun = hostOverrides[host]?.get(step::class)?.also {
            log.info { "Found override for: $host on ${it::class.simpleName}" }
        } ?: step

        return try {
            monitor.meter.timer("${stepToRun.metricPrefix}.latency").recordCallable {
                stepToRun.process(pipelineItem, monitor).also {
                    monitor.meter.counter("${stepToRun.metricPrefix}.success").increment()
                }
            }!!
        } catch (e: Exception) {
            val qualifiers = arrayOf("type", e::class.simpleName, "queue", pipelineItem.task.source)
            monitor.meter.counter("Pipeline.exception", *qualifiers)
            monitor.meter.counter("${stepToRun.metricPrefix}.exception", *qualifiers).increment()
            log.error(e) { "Failed to run step: ${stepToRun::class.simpleName}" }
            pipelineItem
        }
    }

    override fun then(step: Step<T>): ChainedRoute<T> {
        return firstRun(step)
    }

    override fun finally(step: Step<T>) {
        this.finallyStep = step
    }
}


fun pipeline(init: Pipeline.() -> Unit): Pipeline {
    return Pipeline().apply(init)
}


