package com.francisbailey.summitsearch.index.worker.indexing.override

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.StepTest
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.override.MountainProjectContentValidatorStep
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Duration
import java.util.*

class MountainProjectContentValidatorStepTestpackage: StepTest() {

    private val step = MountainProjectContentValidatorStep()

    @Test
    fun `skips route pages that do not contain alpine routes`() {
        val document = loadHtml("mountainproject/non-alpine-route.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl =  URL("https://www.mountainproject.com/route/109394189/skullduggery"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem(
            task = task,
            payload = DatedDocument(
                pageCreationDate = null,
                document = document
            )
        )

        val result = step.process(item, monitor)

        assertFalse(result.continueProcessing)
    }

    @Test
    fun `skips route type check on non-route pages`() {
        val document = loadHtml("mountainproject/area.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl =  URL("https://www.mountainproject.com/area/116571347/aguja-de-is"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem(
            task = task,
            payload = DatedDocument(
                pageCreationDate = null,
                document = document
            )
        )

        val result = step.process(item, monitor)

        assertTrue(result.continueProcessing)
    }

    @Test
    fun `allows alpine route pages`() {
        val document = loadHtml("mountainproject/route.html")
        val task = IndexTask(
            source = "some-queue",
            details = IndexTaskDetails(
                entityUrl =  URL("https://www.mountainproject.com/route/121351972/cara-este"),
                submitTime = Date().time,
                taskRunId = "test123",
                taskType = IndexTaskType.HTML,
                entityTtl = Duration.ofMinutes(60).seconds,
                id = "1234656"
            )
        )

        val item = PipelineItem(
            task = task,
            payload = DatedDocument(
                pageCreationDate = null,
                document = document
            )
        )

        val result = step.process(item, monitor)
        assertTrue(result.continueProcessing)
    }
}