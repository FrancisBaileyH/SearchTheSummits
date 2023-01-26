package com.francisbailey.summitsearch.index.worker.extension

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ElementExtensionTest {


    @Test
    fun `only fetches images wrapped by a figure and with a figcaption`() {
        val html = """
           <html>
                <body>
                    <p>Test <img id="bad-image" src="some-source" /></p>
                    <figure>
                        <img id="good-image" src="a-good-source.png" />
                        <figcaption>Hello world</figcaption>
                    </figure>
                    <figure>
                        <img id="bad-image-without-caption" src="some-other-source" />
                    </figure>
                </body>
           </html>
        """

        val document = Jsoup.parse(html)
        val images = document.getCaptionedImages()

        assertEquals(1, images.size)
        assertEquals("a-good-source.png", images.first().imageSrc)
    }

    @Test
    fun `skips images that do not have a png or jpeg extension`() {
        val html = """
           <html>
                <body>
                    <figure>
                        <img id="good-image" src="a-good-source.gif" />
                        <figcaption>Hello world</figcaption>
                    </figure>
                </body>
           </html>
        """

        val document = Jsoup.parse(html)
        val images = document.getCaptionedImages()

        assertTrue(images.isEmpty())
    }

    @Test
    fun `fetches og image`() {
        val html = """
            <html>
            <head>
            <meta property="og:image" content="https://www.somesite.com/image.jpeg" />
            <meta property="og:image:alt" content="Test Caption" />
            </head>
            <body>
                <p>YEP</p>
            </body>
            </html>
        """

        val document = Jsoup.parse(html)

        val captionedImage = document.getOGImage()

        assertEquals("https://www.somesite.com/image.jpeg", captionedImage?.imageSrc)
        assertEquals("Test Caption", captionedImage?.caption)
    }

}