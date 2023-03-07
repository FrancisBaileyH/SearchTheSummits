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
        val images = document.getFigCaptionedImages()

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
        val images = document.getCaptionedImages("figure", "figcaption")

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

    @Test
    fun `fetches word press captions`() {
        val html = """
           <html>
                <body>
                    <div class="wp-caption aligncenter">
                        <a href="#"><img id="good-image" src="a-good-source.PNG" /></a>
                        <p class="wp-caption-text">Hello world</p>
                    </div>
                </body>
           </html>
        """.trimIndent()

        val document = Jsoup.parse(html)
        val captionedImage = document.getWPCaptionedImages().first()

        assertEquals("a-good-source.PNG", captionedImage.imageSrc)
        assertEquals("Hello world", captionedImage.caption)
    }

    @Test
    fun `fetches dl captioned images`() {
        val html = """
            <html>
                <body>
                    <div>
                        <dl id="attachment_4741">
                            <dt><img decoding="async" class="alignnone lazy loaded" src="https://i0.wp.com/trailcatjim.com/wp-content/uploads/2015/08/IMG_2322.jpg?resize=1170%2C878&amp;ssl=1" data-src="https://i0.wp.com/trailcatjim.com/wp-content/uploads/2015/08/IMG_2322.jpg?resize=1170%2C878&amp;ssl=1" alt="Welch Peak as seen from Williamson Lake in the Cheam Range" data-was-processed="true" width="750" height="563"></dt>
                            <dd>Welch Peak From Williamson Lake Camp</dd>
                        </dl>
                    </div>
                </body>
            </html>    
        """.trimIndent()

        val document = Jsoup.parse(html)
        val captionedImage = document.getDlCaptionedImages().first()

        assertEquals("https://i0.wp.com/trailcatjim.com/wp-content/uploads/2015/08/IMG_2322.jpg?resize=1170%2C878&ssl=1", captionedImage.imageSrc)
        assertEquals("Welch Peak From Williamson Lake Camp", captionedImage.caption)
    }

    @Test
    fun `fetches dl captioned images with data-src`() {
        val html = """
            <html>
                <body>
                    <div>
                        <dl id="attachment_7510">
                            <dt><img decoding="async" class="alignnone lazy" src="data:image/svg+xml,%3Csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%201170%20878'%3E%3C/svg%3E" data-src="https://i0.wp.com/trailcatjim.com/wp-content/uploads/2012/09/IMG_7710.jpg?resize=1170%2C878&#038;ssl=1" alt="mountain climbers camp in a grassy basin with linguring snow patches in the Mt Baker Wilderness" width="1170" height="878"  data-recalc-dims="1"></dt>
                            <dd>Camp In Gargett Basin</dd>
                        </dl>
                    </div>
                </body>
            </html>    
        """.trimIndent()

        val document = Jsoup.parse(html)
        val captionedImage = document.getDlCaptionedImages().first()

        assertEquals("https://i0.wp.com/trailcatjim.com/wp-content/uploads/2012/09/IMG_7710.jpg?resize=1170%2C878&ssl=1", captionedImage.imageSrc)
        assertEquals("Camp In Gargett Basin", captionedImage.caption)
    }

}