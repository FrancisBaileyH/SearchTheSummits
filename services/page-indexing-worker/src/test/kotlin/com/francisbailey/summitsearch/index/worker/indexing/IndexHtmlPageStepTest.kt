package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.step.DatedDocument
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*


class IndexHtmlPageStepTest: StepTest() {

    private val indexService = mock<DocumentIndexService>()

    private val documentIndexFilterService = mock<DocumentFilterService> {
        on(mock.shouldFilter(any())).thenReturn(false)
    }

    private val step = IndexHtmlPageStep(
        documentIndexService = indexService,
        documentIndexingFilterService = documentIndexFilterService
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
        val html = """
            <html>
                <head>
                    <title>Test Page!</title>
                    <meta name="description" content="this is a test">
                </head>
                <body>
                    <p>Test content.</p>
                    <div>Hello world</div>
                    <footer><h1>Footer</h1></footer>
                </body>
            </html>
        """

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse(html)
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
        val html = """
           <html>
             <body>
               <p>Some Test</p>
             </body>
           </html>
        """

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse(html)
            )
        )

        step.process(pipelineItem, monitor)

        verify(indexService).indexContent(check<DocumentPutRequest> {
            assertEquals(defaultIndexTask.details.entityUrl.host, it.title)
        })
    }

    @Test
    fun `excluded tags are removed before text is indexed`() {
        val htmlWithExcludedTags = """
            <html>
                <body>
                    <header><p>Here is header</p></header>
                    <nav><p>Some Nav Thing</p></nav>
                    <p>Test content.</p>
                    <ul>
                      <li><a>Bad Content</a></li>
                    </ul>
                    <footer><h1>Footer</h1></footer>
                </body>
            </html>
        """.trimIndent()

        val pipelineItem = PipelineItem(
            task = defaultIndexTask,
            payload = DatedDocument(
                pageCreationDate = null,
                document = Jsoup.parse(htmlWithExcludedTags)
            )
        )

        step.process(pipelineItem, monitor)

        verify(indexService).indexContent(check<DocumentPutRequest> {
            assertEquals("Test content.", it.rawTextContent)
            assertEquals("Test content.", it.paragraphContent)
        })
    }
}