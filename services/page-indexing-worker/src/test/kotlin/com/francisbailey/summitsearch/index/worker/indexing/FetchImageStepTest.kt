package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.crawler.ImageCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.NonRetryableEntityException
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchImageStep
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun `returns image from url`() {
        whenever(imageCrawlerService.get(any())).thenReturn(image)
        val modifiedItem = step.process(item, monitor)
        assertEquals(image, modifiedItem.payload)
        assertFalse(modifiedItem.shouldRetry)
        assertTrue(modifiedItem.continueProcessing)
    }

    @Test
    fun `when retryable entity exception occurs allow retry`() {
        whenever(imageCrawlerService.get(any())).thenThrow(RetryableEntityException("Test Failure"))
        assertThrows<RetryableEntityException> { step.process(item, monitor) }
    }

    @Test
    fun `when non-retryable entity exception occurs do not allow retry`() {
        whenever(imageCrawlerService.get(any())).thenThrow(NonRetryableEntityException("Test Failure"))
        assertThrows<NonRetryableEntityException> { step.process(item, monitor) }
    }
}