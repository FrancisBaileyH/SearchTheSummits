package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexPDFStep
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.net.URL


class IndexPDFStepTest: StepTest() {

    private val summitSearchIndexService = mock<SummitSearchIndexService>()

    private val textStripper = mock<PDFTextStripper>()

    private val textStripperGenerator = mock<() -> PDFTextStripper> {
        on(mock.invoke()).thenReturn(textStripper)
    }

    private val step = IndexPDFStep(summitSearchIndexService = summitSearchIndexService, textStripper = textStripperGenerator)

    private val document = mock<PDDocument>()

    private val task = IndexTask(
        source = "test",
        details = IndexTaskDetails(
            id = "123",
            taskRunId = "1234",
            pageUrl = URL("https://francisbaileyh.com/test%20value.pdf"),
            taskType = IndexTaskType.PDF,
            refreshIntervalSeconds = 123L,
            submitTime = 1234L
        )
    )

    private val item = PipelineItem(task = task, payload = document)


    @Test
    fun `saves document with url decoded title`() {
        val content = "Some Test Content Here"

        whenever(textStripper.getText(any())).thenReturn(content)

        val result = step.process(item, monitor)

        verify(summitSearchIndexService).indexPageContents(check<SummitSearchIndexRequest> {
            assertEquals("test value", it.title)
            assertEquals("", it.seoDescription)
            assertEquals("", it.paragraphContent)
            assertEquals(content, it.rawTextContent)
            assertEquals(task.details.pageUrl, it.source)
        })

        assertTrue(result.continueProcessing)
        assertFalse(result.canRetry)
    }

}