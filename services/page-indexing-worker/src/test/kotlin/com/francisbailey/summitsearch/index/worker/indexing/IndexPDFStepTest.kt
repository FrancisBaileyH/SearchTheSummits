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

    private val summitSearchIndexService = mock<SummitSearchIndexService> {
        on(mock.maxBulkIndexRequests).thenReturn(100)
    }

    private val textStripper = mock<PDFTextStripper>()

    private val textStripperGenerator = mock<() -> PDFTextStripper> {
        on(mock.invoke()).thenReturn(textStripper)
    }

    private val pdfPartitionThreshold = 3

    private val step = IndexPDFStep(
        summitSearchIndexService = summitSearchIndexService,
        textStripper = textStripperGenerator,
        pdfPagePartitionThreshold = pdfPartitionThreshold
    )

    private val document = mock<PDDocument>()

    private val task = IndexTask(
        source = "test",
        details = IndexTaskDetails(
            id = "123",
            taskRunId = "1234",
            entityUrl =  URL("https://francisbaileyh.com/test%20value.pdf"),
            taskType = IndexTaskType.PDF,
            entityTtl = 123L,
            submitTime = 1234L
        )
    )

    private val item = PipelineItem(task = task, payload = document)


    @Test
    fun `saves document with url decoded title using put call`() {
        val content = "Some Test Content Here"

        whenever(document.numberOfPages).thenReturn(pdfPartitionThreshold.dec())
        whenever(textStripper.getText(any())).thenReturn(content)

        val result = step.process(item, monitor)

        verify(summitSearchIndexService).indexContent(check<SummitSearchIndexRequest> {
            assertEquals("test value", it.title)
            assertEquals("", it.seoDescription)
            assertEquals("", it.paragraphContent)
            assertEquals(content, it.rawTextContent)
            assertEquals(task.details.entityUrl, it.source)
        })

        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)
    }

    @Test
    fun `partitions pdf when more than pdf partition threshold pages present`() {
        val content = "Some Test Content Here"
        val content2 = "Some other content"

        whenever(document.numberOfPages).thenReturn(pdfPartitionThreshold.inc())
        whenever(textStripper.getText(any()))
            .thenReturn(content)
            .thenReturn(content2)

        val result = step.process(item, monitor)

        argumentCaptor<List<SummitSearchIndexRequest>> {
            verify(summitSearchIndexService).indexPartitionedContent(capture())

            assertEquals(firstValue.first(), SummitSearchIndexRequest(
                title = "test value (pages 1-3)",
                seoDescription = "",
                paragraphContent = "",
                source = URL("${task.details.pageUrl}#page=1"),
                rawTextContent = content
            ))

            assertEquals(firstValue[1], SummitSearchIndexRequest(
                title = "test value (page 4)",
                seoDescription = "",
                paragraphContent = "",
                source = URL("${task.details.pageUrl}#page=4"),
                rawTextContent = content2
            ))
        }

        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)
    }

    @Test
    fun `partitions pdf with title page 1,2 if only two pages in partition`() {
        val content = "Some Test Content Here"
        val content2 = "Some other content"

        whenever(document.numberOfPages).thenReturn(pdfPartitionThreshold + 2)
        whenever(textStripper.getText(any()))
            .thenReturn(content)
            .thenReturn(content2)

        val result = step.process(item, monitor)

        argumentCaptor<List<SummitSearchIndexRequest>> {
            verify(summitSearchIndexService).indexPartitionedContent(capture())

            assertEquals(firstValue.first(), SummitSearchIndexRequest(
                title = "test value (pages 1-3)",
                seoDescription = "",
                paragraphContent = "",
                source = URL("${task.details.pageUrl}#page=1"),
                rawTextContent = content
            ))

            assertEquals(firstValue[1], SummitSearchIndexRequest(
                title = "test value (pages 4,5)",
                seoDescription = "",
                paragraphContent = "",
                source = URL("${task.details.pageUrl}#page=4"),
                rawTextContent = content2
            ))
        }

        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)
    }

    @Test
    fun `partitions requests to index when they exceed bulk request threshold`() {
        val content = "Some Test Content Here"
        val content2 = "Some other content"

        whenever(summitSearchIndexService.maxBulkIndexRequests).thenReturn(2)
        whenever(document.numberOfPages).thenReturn(6 * pdfPartitionThreshold)
        whenever(textStripper.getText(any()))
            .thenReturn(content)
            .thenReturn(content2)

        val result = step.process(item, monitor)

        verify(summitSearchIndexService, times(3)).indexPartitionedContent(any())

        assertTrue(result.continueProcessing)
        assertFalse(result.shouldRetry)
    }
}