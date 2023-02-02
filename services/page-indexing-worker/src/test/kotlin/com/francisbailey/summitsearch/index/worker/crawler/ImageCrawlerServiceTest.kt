package com.francisbailey.summitsearch.index.worker.crawler

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.ImmutableImageLoader
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.URL

class ImageCrawlerServiceTest {

    private val httpCrawlerClient = mock<HttpCrawlerClient>()

    private val immutableImageLoader = mock<ImmutableImageLoader>()

    private val image = mock<ImmutableImage>()

    private val imageCrawler = ImageCrawlerService(httpCrawlerClient, immutableImageLoader)

    private val url = URL("https://francisbaileyh.com")


    @Test
    fun `returns immutable image fetched successfully`() {
        val content = ByteArray(1)
        val response = getResponse(url, content, ContentType.Image.Any)

        whenever(httpCrawlerClient.get<ImmutableImage>(any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> ImmutableImage>(2).invoke(response)
        }

        whenever(immutableImageLoader.fromBytes(any())).thenReturn(image)

        val result = imageCrawler.get(url)

        assertEquals(image, result)
        verify(immutableImageLoader).fromBytes(content)
    }


    @Test
    fun `throws UnparsableEntityException when content validation fails`() {
        val content = ByteArray(1)
        val response = getResponse(url, content, ContentType.Text.Html)

        whenever(httpCrawlerClient.get<ImmutableImage>(any(), any(), any())).then {
            it.getArgument<(HttpResponse) -> Unit>(1).invoke(response)
            it.getArgument<(HttpResponse) -> ImmutableImage>(2).invoke(response)
        }

        assertThrows<UnparsableEntityException> { imageCrawler.get(url) }
    }
}