package com.francisbailey.summitsearch.index.worker.crawler

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.net.URL

@Service
class ImageCrawlerService(
    private val httpCrawlerClient: HttpCrawlerClient,
    private val imageLoader: ImmutableImageLoader
) {
    private val transformer: (HttpResponse) -> ImmutableImage = {
        val imageData = runBlocking { it.body<ByteArray>() }
        imageLoader.fromBytes(imageData)
    }

    private val validator: (HttpResponse) -> Unit = {
        val isImage = it.contentType()?.match(ContentType.Image.Any) ?: false

        if (!isImage) {
            throw UnparsableEntityException("Content type is not image: ${it.contentType()}")
        }
    }

    fun get(imageUrl: URL): ImmutableImage {
         return httpCrawlerClient.getContent(imageUrl, responseValidationInterceptor = validator, transformer = transformer)
    }
}