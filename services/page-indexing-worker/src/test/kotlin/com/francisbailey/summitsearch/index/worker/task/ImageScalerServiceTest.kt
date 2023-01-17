package com.francisbailey.summitsearch.index.worker.task

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import com.sksamuel.scrimage.nio.PngWriter
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ImageScalerServiceTest {


    private val imageLoader = mock<ImmutableImageLoader>()

    private val imageWriter = mock<PngWriter>()

    private val imageScaler = ImageScalerService(imageLoader, imageWriter)

    private val originalImage = mock<ImmutableImage>()

    private val scaledImage = mock<ImmutableImage>()


    @Test
    fun `scales image to thumbnail size`() {
        whenever(imageLoader.fromBytes(any())).thenReturn(originalImage)
        whenever(originalImage.scaleToHeight(any())).thenReturn(scaledImage)
        whenever(scaledImage.bytes(any())).thenReturn(ByteArray(2))

        imageScaler.createThumbnailFromBytes(ByteArray(1))

        verify(originalImage).scaleToHeight(92)
        verify(scaledImage).bytes(imageWriter)
    }

}