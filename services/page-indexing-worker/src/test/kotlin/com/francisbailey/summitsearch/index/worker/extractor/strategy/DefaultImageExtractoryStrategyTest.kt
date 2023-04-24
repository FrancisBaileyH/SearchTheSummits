package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.CaptionedImage
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultImageExtractoryStrategyTest {

    private val extractor = DefaultImageExtractorStrategy()

    @Test
    fun `extracts expected content`() {
        val html = """
                <figure>
                    <img id="good-image" src="a-good-source.png" />
                    <figcaption>Hello world</figcaption>
                </figure>
                <figure>
                    <img id="bad-image-without-caption" src="some-other-source" />
                </figure>
                <div class="wp-caption aligncenter">
                    <a href="#"><img id="good-image" src="another-good-source.png" /></a>
                    <p class="wp-caption-text">Hello world 2</p>
                </div>
        """.trimIndent()

        val expectedDiscoveries = setOf(
            CaptionedImage(
                imageSrc = "a-good-source.png",
                caption = "Hello world",
            ),
            CaptionedImage(
                imageSrc = "another-good-source.png",
                caption = "Hello world 2",
            )
        )

        assertEquals(expectedDiscoveries, extractor.extract(Jsoup.parse(html)).toSet())
    }
}