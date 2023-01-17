package com.francisbailey.summitsearch.index.worker.task

import com.sksamuel.scrimage.nio.ImageWriter
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import org.springframework.stereotype.Service


@Service
class ImageScalerService(
    private val imageLoader: ImmutableImageLoader,
    private val imageWriter: ImageWriter
) {

    fun createThumbnailFromBytes(imageData: ByteArray): ByteArray {
        return createThumbnailFromBytes(imageData, DEFAULT_THUMBNAIL_HEIGHT)
    }

    fun createThumbnailFromBytes(imageData: ByteArray, height: Int): ByteArray {
        val image = imageLoader.fromBytes(imageData)
        val scaledImage = image.scaleToHeight(height)

        return scaledImage.bytes(imageWriter)
    }

    companion object {
        private const val DEFAULT_THUMBNAIL_HEIGHT = 92
    }

}