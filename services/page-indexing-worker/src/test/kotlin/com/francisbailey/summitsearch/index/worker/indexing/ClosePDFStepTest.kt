package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.ClosePDFStep
import org.apache.pdfbox.pdmodel.PDDocument
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.URL

class ClosePDFStepTest: StepTest() {

    private val document = mock<PDDocument>()

    private val step = ClosePDFStep()

    private val task = IndexTask(
        source = "test",
        details = IndexTaskDetails(
            id = "123",
            taskRunId = "1234",
            pageUrl = URL("https://francisbaileyh.com/test.pdf"),
            taskType = IndexTaskType.PDF,
            refreshIntervalSeconds = 123L,
            submitTime = 1234L
        )
    )

    @Test
    fun `closes document`() {
        step.process(PipelineItem(task, document), monitor)
        verify(document).close()
    }

}