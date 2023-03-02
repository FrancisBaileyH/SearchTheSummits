package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.extension.stripQueryAndFragment
import com.francisbailey.summitsearch.index.worker.indexing.step.CheckImageExistsStep
import com.francisbailey.summitsearch.index.worker.store.ImageStoreType
import com.francisbailey.summitsearch.index.worker.store.ImageWriterStore
import com.sksamuel.scrimage.ImmutableImage
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckImageExistsStepTest: StepTest() {

    private val imageWriterStore = mock<ImageWriterStore>()

    private val step = CheckImageExistsStep(imageWriterStore)

    private val item = PipelineItem<ImmutableImage>(
        payload = null,
        task = defaultIndexTask
    )
    @Test
    fun `aborts processing if exception thrown when checking image store`() {
        whenever(imageWriterStore.exists(any(), any())).thenThrow(RuntimeException("Test"))
        assertFalse(step.process(item, monitor).continueProcessing)
    }

    @Test
    fun `aborts processing if image already exists in store`() {
        whenever(imageWriterStore.exists(any(), any())).thenReturn(true)
        assertFalse(step.process(item, monitor).continueProcessing)
    }

    @Test
    fun `continues processing if image does not exist in store`() {
        whenever(imageWriterStore.exists(any(), any())).thenReturn(false)
        assertTrue(step.process(item, monitor).continueProcessing)

        verify(imageWriterStore).exists(defaultIndexTask.details.pageUrl.stripQueryAndFragment(), ImageStoreType.STANDARD)
    }

}