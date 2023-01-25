package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.configuration.PipelineConfiguration
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchHtmlPageStep
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitLinksStep
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.URL
import java.time.Duration
import java.util.*

class PipelineConfigurationTest: StepTest() {

    private val fetchHtmlPageStep = mock<FetchHtmlPageStep>()

    private val submitLinksStep = mock<SubmitLinksStep>()

    private val indexHtmlPageStep = mock<IndexHtmlPageStep>()

    private val pipelineConfiguration = PipelineConfiguration(
        fetchHtmlPageStep = fetchHtmlPageStep,
        submitLinksStep = submitLinksStep,
        indexHtmlPageStep = indexHtmlPageStep
    )

    @Test
    fun `executes steps in expected order for html route`() {
        val task = IndexTask(
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

        val pipelineItem = PipelineItem<Document>(task = task, payload = null)

        whenever(fetchHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(submitLinksStep.process(any(), any())).thenReturn(pipelineItem)
        whenever(indexHtmlPageStep.process(any(), any())).thenReturn(pipelineItem)

        val pipeline = pipelineConfiguration.indexingPipeline()

        pipeline.process(task, monitor)

        inOrder(fetchHtmlPageStep, submitLinksStep, indexHtmlPageStep) {
            verify(fetchHtmlPageStep).process(any(), any())
            verify(submitLinksStep).process(any(), any())
            verify(indexHtmlPageStep).process(any(), any())
        }
    }
}