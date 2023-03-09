package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.GenerateImagePreviewStep
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GenerateImagePreviewStepTest: StepTest() {

    private val image = mock<ImmutableImage>()

    private val step = GenerateImagePreviewStep()

    private val item = PipelineItem<ImmutableImage>(
        payload = image,
        task = defaultIndexTask
    )

    @Test
    fun `should scale image to 92 pixels in height`() {
        val modifiedItem = step.process(item, monitor)

        verify(image).scaleToHeight(200)
        assertTrue(modifiedItem.continueProcessing)
    }

    @Test
    fun `should stop processing if scaling fails`() {
        whenever(image.scaleToHeight(any())).thenThrow(RuntimeException("Test"))
        assertThrows<RuntimeException> { step.process(item, monitor) }
    }
}