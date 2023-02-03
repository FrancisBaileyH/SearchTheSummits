package com.francisbailey.summitsearch.index.worker.indexing.override

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Duration
import java.util.*

class PeakBaggerContentValidatorStepTest: StepTest() {

    private val step = PeakBaggerContentValidatorStep()


    @Test
    fun `skips content that is less than 400 characters in length`() {
        val document = loadHtml("peakbagger/TripReportWithoutMinimumContent.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                pageUrl = URL("https://peakbagger.com/climber/ascent.aspx?aid=1761962"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem<Document>(task = task, payload = document)

        val result = step.process(item, monitor)

        assertFalse(result.continueProcessing)
    }

    @Test
    fun `does not check non ascent aspx pages`() {
        val document = loadHtml("peakbagger/TripReportWithoutMinimumContent.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                pageUrl = URL("https://peakbagger.com/some/otherpage.aspx"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem<Document>(task = task, payload = document)

        val result = step.process(item, monitor)

        assertTrue(result.continueProcessing)
    }

    @Test
    fun `allows content that is more than 400 characters in length`() {
        val document = loadHtml("peakbagger/TripReportWithMinimumContent.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                pageUrl = URL("https://peakbagger.com/climber/ascent.aspx?aid=1761962"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                refreshIntervalSeconds = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem<Document>(task = task, payload = document)

        val result = step.process(item, monitor)

        assertTrue(result.continueProcessing)
    }
}