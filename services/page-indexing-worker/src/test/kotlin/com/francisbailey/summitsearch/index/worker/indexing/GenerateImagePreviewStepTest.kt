package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.indexing.step.GenerateImagePreviewStep
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GenerateImagePreviewStepTest: StepTest() {

    private val image = ImmutableImage.create(
        GenerateImagePreviewStep.MIN_WIDTH + 5,
        GenerateImagePreviewStep.MIN_HEIGHT + 5,
    )

    private val step = GenerateImagePreviewStep()

    private val item = PipelineItem(
        payload = image,
        task = defaultIndexTask
    )

    @Test
    fun `should scale image to 200 pixels in height`() {
        val modifiedItem = step.process(item, monitor)

        assertEquals(GenerateImagePreviewStep.DEFAULT_HEIGHT, modifiedItem.payload?.height)
        assertTrue(modifiedItem.continueProcessing)
    }

    @Test
    fun `aborts processing if image is less than min height`() {
        item.payload = ImmutableImage.create(
            GenerateImagePreviewStep.MIN_WIDTH,
            GenerateImagePreviewStep.MIN_HEIGHT - 1
        )
        val modifiedItem = step.process(item, monitor)

        assertFalse(modifiedItem.continueProcessing)
    }

    @Test
    fun `aborts processing if image is less than min width`() {
        item.payload = ImmutableImage.create(
            GenerateImagePreviewStep.MIN_WIDTH - 1,
            GenerateImagePreviewStep.MIN_HEIGHT
        )
        val modifiedItem = step.process(item, monitor)

        assertFalse(modifiedItem.continueProcessing)
    }
}