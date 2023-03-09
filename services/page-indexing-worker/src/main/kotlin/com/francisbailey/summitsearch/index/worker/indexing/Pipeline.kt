package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.micrometer.core.instrument.MeterRegistry
import mu.KLogger
import mu.KotlinLogging
import software.amazon.awssdk.core.exception.NonRetryableException
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
    var continueProcessing: Boolean = false,
    var canRetry: Boolean = true
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


    /**
     * For now, an exception does not stop continuation of the pipeline. Each step
     * must explicitly call for process continuation to stop. This can be changed in
     * the future, but for now we'll leave it as is.
     */
    fun execute(task: IndexTask, monitor: PipelineMonitor): PipelineItem<T> {
        val meter = monitor.meter
        var item: PipelineItem<T> = PipelineItem(task, null)

        for (nextStep in steps) {
            val step = getOverrideOrElse(nextStep, item)
            println(step.metricPrefix)
            val tags = arrayOf("step", step.metricPrefix, "queue", task.source)

            item = executeStep(step, item, monitor, *tags)

            if (!item.continueProcessing) {
                meter.counter("pipeline.aborted", *tags).increment()
                break
            }
        }

        finallyStep?.let {
            item = executeStep(it, item, monitor)
        }

        return item
    }

    private fun executeStep(step: Step<T>, item: PipelineItem<T>, monitor: PipelineMonitor, vararg tags: String): PipelineItem<T> {
        val meter = monitor.meter
        var processedItem = item

        try {
            meter.timer("pipeline.step.latency", *tags).recordCallable {
                processedItem = step.process(item, monitor)
                meter.counter("pipeline.step.success", *tags).increment()
            }
        } catch (e: Exception) {
            if (e is NonRetryableException) {
                meter.counter("pipeline.step.noretry", *tags).increment()
                processedItem.canRetry = false
            }

            meter.counter("pipeline.step.failure", *tags, "exception", e::class.simpleName).increment()
            processedItem.continueProcessing = false
        }

        return processedItem
    }

    private fun getOverrideOrElse(step: Step<T>, item: PipelineItem<T>): Step<T> {
        val host = item.task.details.pageUrl.host
        return hostOverrides[host]?.get(step::class) ?: step
    }

    override fun withHostOverride(host: String, targetStep: KClass<*>, override: Step<T>): ChainedRoute<T> {
        val mappedOverride = hostOverrides.getOrPut(host) {
            mutableMapOf()
        }

        mappedOverride[targetStep] = override
        return this
    }

    override fun firstRun(step: Step<T>): ChainedRoute<T> {
        steps.add(step)
        return this
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


