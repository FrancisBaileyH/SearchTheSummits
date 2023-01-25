package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.index.worker.task.DocumentFilterService
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class IndexHtmlPageStepTest: StepTest() {

    private val indexService = mock<SummitSearchIndexService>()

    private val documentIndexFilterService = mock<DocumentFilterService> {
        on(mock.shouldFilter(any())).thenReturn(false)
    }

    private val step = IndexHtmlPageStep(
        summitSearchIndexService = indexService,
        documentIndexingFilterService = documentIndexFilterService
    )

    @Test
    fun `indexes page contents if filter does not apply`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = htmlContent
        )

        val result = step.process(pipelineItem, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.canRetry)

        verify(depencencyCircuitBreaker).executeCallable<Unit>(any())
        verify(documentIndexFilterService).shouldFilter(defaultIndexTask.details.pageUrl)
        verify(indexService).indexPageContents(SummitSearchIndexRequest(defaultIndexTask.details.pageUrl, htmlContent))
    }

    @Test
    fun `does not index page contents if filter applies`() {
        val htmlContent = Jsoup.parse("<html>Some Web Page</html>")

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = htmlContent
        )

        whenever(documentIndexFilterService.shouldFilter(any())).thenReturn(true)

        val result = step.process(pipelineItem, monitor)

        Assertions.assertTrue(result.continueProcessing)
        Assertions.assertFalse(result.canRetry)

        verify(documentIndexFilterService).shouldFilter(defaultIndexTask.details.pageUrl)
        verifyNoInteractions(indexService)
    }
}