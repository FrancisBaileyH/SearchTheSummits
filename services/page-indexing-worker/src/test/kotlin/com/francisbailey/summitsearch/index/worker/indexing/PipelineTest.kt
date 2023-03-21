package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.net.URL
import java.time.Duration
import java.util.*


class PipelineTest: StepTest() {

    abstract class TestStep: Step<Unit>
    abstract class OverrideStep: Step<Unit>

    private val step = mock<TestStep> {
        on(mock.metricPrefix).thenReturn("Step")
    }

    private val nextStep = mock<Step<Unit>> {
        on(mock.metricPrefix).thenReturn("NextStep")
    }

    private val overrideStep = mock<OverrideStep> {
        on(mock.metricPrefix).thenReturn("OverrideStep")
    }

    private val finalStep =  mock<Step<Unit>> {
        on(mock.metricPrefix).thenReturn("FinalStep")
    }

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
            entityUrl =  URL("https://www.francisbaileyh.com"),
            submitTime = Date().time,
            taskRunId = "test123",
            taskType = IndexTaskType.PDF,
            entityTtl = Duration.ofMinutes(60).seconds
        )
    )

    @Test
    fun `throws exception if route is unmapped`() {
        val badTask = IndexTask(
            messageHandle = "testHandle123",
            source = "some-queue-name",
            details = IndexTaskDetails(
                id = "123456",
                entityUrl =  URL("https://www.francisbaileyh.com"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.IMAGE,
                entityTtl = Duration.ofMinutes(60).seconds
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
        whenever(nextStep.process(any(), any())).thenReturn(item)

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
                    .withHostOverride(task.details.entityUrl.host, step::class, overrideStep)
            }
        }

        val item = PipelineItem<Unit>(
            task = task,
            payload = null,
            continueProcessing = false
        )

        whenever(overrideStep.process(any(), any())).thenReturn(item.apply { continueProcessing = true })
        whenever(nextStep.process(any(), any())).thenReturn(item.apply { continueProcessing = true })

        pipeline.process(task, monitor)

        verifyNoInteractions(step)

        verify(overrideStep).process(org.mockito.kotlin.check {
            assertEquals(item.apply { continueProcessing = false }, it)
        }, any())

        verify(nextStep).process(org.mockito.kotlin.check {
            assertEquals(item.apply { continueProcessing = false }, it)
        }, any())
    }

    @Test
    fun `always executes finally step`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
                    .finally(finalStep)
            }
        }

        val item = PipelineItem<Unit>(
            task = task,
            payload = null,
            continueProcessing = true
        )

        whenever(step.process(any(), any())).thenReturn(item.apply { continueProcessing = false })
        whenever(finalStep.process(any(), any())).thenReturn(item)

        pipeline.process(task, monitor)

        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())

        verifyNoInteractions(nextStep)

        verify(finalStep).process(org.mockito.kotlin.check {
            assertEquals(item, it)
        }, any())
    }

    @Test
    fun `does not retry on NonRetryableEntityException`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
            }
        }

        whenever(step.process(any(), any())).thenThrow(NonRetryableEntityException("Test"))

        val result = pipeline.process(task, monitor)

        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())

        verifyNoInteractions(nextStep)
        assertFalse(result)
    }

    @Test
    fun `does retry on other exceptions`() {
        val pipeline = pipeline {
            route(IndexTaskType.PDF) {
                firstRun(step)
                    .then(nextStep)
            }
        }

        whenever(step.process(any(), any())).thenThrow(RuntimeException("Test"))

        val result = pipeline.process(task, monitor)

        verify(step).process(org.mockito.kotlin.check {
            assertEquals(task, it.task)
        }, any())

        verifyNoInteractions(nextStep)
        assertTrue(result)
    }

}