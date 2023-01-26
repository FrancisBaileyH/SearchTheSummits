package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.GenerateThumbnailStep
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class GenerateThumbnailStepTest: StepTest() {

    private val image = mock<ImmutableImage>()

    private val step = GenerateThumbnailStep()

    private val item = PipelineItem<ImmutableImage>(
        payload = image,
        task = defaultIndexTask
    )

    @Test
    fun `should scale image to 92 pixels in height`() {
        val modifiedItem = step.process(item, monitor)

        verify(image).scaleToHeight(92)
        assertTrue(modifiedItem.continueProcessing)
    }

    @Test
    fun `should stop processing if scaling fails`() {
        whenever(image.scaleToHeight(any())).thenThrow(RuntimeException("Test"))

        val modifiedItem = step.process(item, monitor)

        assertFalse(modifiedItem.continueProcessing)
    }


}