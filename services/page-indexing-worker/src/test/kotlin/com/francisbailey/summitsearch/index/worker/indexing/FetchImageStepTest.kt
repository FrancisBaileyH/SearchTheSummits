package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.crawler.ImageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchImageStep
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FetchImageStepTest: StepTest() {

    private val image = mock<ImmutableImage>()

    private val imageCrawlerService = mock<ImageCrawlerService>()

    private val step = FetchImageStep(
        imageCrawlerService
    )

    private val item = PipelineItem<ImmutableImage>(
        payload = null,
        task = defaultIndexTask
    )

    /**
     *
    You must write a failing test before you write any production code.
    You must not write more of a test than is sufficient to fail, or fail to compile.
    You must not write more production code than is sufficient to make the currently failing test pass.

     */
    @Test
    fun `returns image from url`() {
        whenever(imageCrawlerService.get(any())).thenReturn(image)
        val modifiedItem = step.process(item, monitor)
        assertEquals(image, modifiedItem.payload)
        assertFalse(modifiedItem.canRetry)
        assertTrue(modifiedItem.continueProcessing)
    }

    @Test
    fun `when retryable entity exception occurs allow retry`() {
        whenever(imageCrawlerService.get(any())).thenThrow(RetryableEntityException("Test Failure"))
        val modifiedItem = step.process(item, monitor)
        assertTrue(modifiedItem.canRetry)
        assertFalse(modifiedItem.continueProcessing)
    }

    @Test
    fun `when non-retryable entity exception occurs do not allow retry`() {
        whenever(imageCrawlerService.get(any())).thenThrow(NonRetryableEntityException("Test Failure"))
        val modifiedItem = step.process(item, monitor)
        assertFalse(modifiedItem.canRetry)
        assertFalse(modifiedItem.continueProcessing)
    }
}