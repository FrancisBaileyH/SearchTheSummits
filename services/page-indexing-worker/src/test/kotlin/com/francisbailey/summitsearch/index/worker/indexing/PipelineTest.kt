package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration
import java.util.*


class PipelineTest: StepTest() {

    abstract class TestStep: Step<Unit>
    abstract class OverrideStep: Step<Unit>

    private val step = mock<TestStep>()

    private val nextStep = mock<Step<Unit>>()

    private val overrideStep = mock<OverrideStep>()

    private val pipeline = pipeline {
        route(IndexTaskType.PDF) {
            firstRun(step)
        }
    }

    private val task = IndexTask(
        messageHandle = "testHandle123",
        source = "some-queue-name",
        details = IndexTaskDetails(
            id = "123456",
            pageUrl = URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.PDF,
            refreshIntervalSeconds = Duration.ofMinutes(60).seconds
        )
    )

    @Test
    fun `throws exception if route is unmapped`() {
        val badTask = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                pageUrl = URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.IMAGE,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds
            )
        )

        assertThrows<IllegalStateException> { pipeline.process(badTask, monitor) }
    }

    @Test
    fun `calls route if it is mapped`() {
        whenever(step.process(any(), any())).thenReturn(PipelineItem(task, null))
        pipeline.process(task, monitor)
        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())
    }

    @Test
    fun `skips next steps if continueProcessing is false`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
            }
        }

        whenever(step.process(any(), any())).thenReturn(
            PipelineItem(
                task = task,
                payload = null,
                continueProcessing = false
            )
        )

        pipeline.process(task, monitor)

        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())

        verifyNoInteractions(nextStep)
    }

    @Test
    fun `calls all steps as long as continue processing is true`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
            }
        }

        val item = PipelineItem<Unit>(
            task = task,
            payload = null,
            continueProcessing = true
        )

        whenever(step.process(any(), any())).thenReturn(item)

        pipeline.process(task, monitor)

        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())

        verify(nextStep).process(org.mockito.kotlin.check {
            assertEquals(item, it)
        }, any())
    }

    @Test
    fun `overrides step on host`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
                    .withHostOverride(task.details.pageUrl.host, step::class, overrideStep)
            }
        }

        val item = PipelineItem<Unit>(
            task = task,
            payload = null,
            continueProcessing = true
        )

        whenever(overrideStep.process(any(), any())).thenReturn(item)
        whenever(overrideStep.metricPrefix).thenReturn("test")

        pipeline.process(task, monitor)

        verifyNoInteractions(step)

        verify(overrideStep).process(org.mockito.kotlin.check {
            assertEquals(item, it)
        }, any())

        verify(nextStep).process(org.mockito.kotlin.check {
            assertEquals(item, it)
        }, any())
    }

}