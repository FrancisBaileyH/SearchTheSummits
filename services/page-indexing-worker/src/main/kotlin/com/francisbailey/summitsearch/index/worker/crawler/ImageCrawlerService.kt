package com.francisbailey.summitsearch.index.worker.crawler

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.net.URL

@Service
class ImageCrawlerService(
    private val httpCrawlerClient: HttpCrawlerClient,
    private val imageLoader: ImmutableImageLoader
) {
    fun get(imageUrl: URL): ImmutableImage {
         return httpCrawlerClient.getContent(imageUrl, responseValidationInterceptor = {
            val isImage = it.contentType()?.match(ContentType.Image.Any) ?: false

            if (!isImage) {
                throw UnparsableEntityException("Content type is not image: ${it.contentType()}")
            }
         },
         transformer = {
             val imageData = runBlocking { it.body<ByteArray>() }
             imageLoader.fromBytes(imageData)
         })
    }
}