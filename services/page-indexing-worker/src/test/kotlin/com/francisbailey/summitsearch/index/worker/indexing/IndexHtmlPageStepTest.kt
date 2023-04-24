package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractor
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*


class IndexHtmlPageStepTest: StepTest() {

    private val indexService = mock<DocumentIndexService>()

    private val extractor = mock<ContentExtractor<DocumentText>>()

    private val documentIndexFilterService = mock<DocumentFilterService> {
        on(mock.shouldFilter(any())).thenReturn(false)
    }

    private val step = IndexHtmlPageStep(
        documentIndexService = indexService,
        documentIndexingFilterService = documentIndexFilterService,
        htmlContentExtractor = extractor
    )

    @Test
    fun `does not index page contents if filter applies`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                htmlContent
            )
        )

        whenever(documentIndexFilterService.shouldFilter(any())).thenReturn(true)

        val result = step.process(pipelineItem, monitor)

        Assertions.assertFalse(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)

        verify(documentIndexFilterService).shouldFilter(defaultIndexTask.details.entityUrl)
        verifyNoInteractions(indexService)
    }

    @Test
    fun `indexes page contents if filter does not apply`() {
        whenever(extractor.extract(any(), any())).thenReturn(
            DocumentText(
                title = "Test Page!",
                description = "this is a test",
                semanticText = "Test content.",
                rawText = "Test content. Hello world"
        ))

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse("")
            )
        )

        val result = step.process(pipelineItem, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.shouldRetry)

        verify(depencencyCircuitBreaker).executeCallable<Unit>(any())
        verify(documentIndexFilterService).shouldFilter(defaultIndexTask.details.entityUrl)
        verify(indexService).indexContent(check {
            assertEquals("Test content. Hello world", it.rawTextContent)
            assertEquals("Test content.", it.paragraphContent)
            assertEquals("this is a test", it.seoDescription)
            assertEquals("Test Page!",it.title)
        })
    }

    @Test
    fun `title fallsback to URL if title value is missing`() {
        whenever(extractor.extract(any(), any())).thenReturn(
            DocumentText(
                title = "",
                description = "this is a test",
                semanticText = "Test content.",
                rawText = "Test content. Hello world"
            ))

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse("")
            )
        )

        step.process(pipelineItem, monitor)

        verify(indexService).indexContent(check {
            assertEquals(defaultIndexTask.details.entityUrl.host, it.title)
        })
    }
}