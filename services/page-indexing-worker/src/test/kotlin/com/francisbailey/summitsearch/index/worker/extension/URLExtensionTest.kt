package com.francisbailey.summitsearch.index.worker.extension

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URL


class URLExtensionTest {

    @Test
    fun `should return url with fragments removed`() {
        val url = URL("https://francisbaileyh.com/test/test3/index.html?query=x#someFragment")
        val expectedUrl = URL("https://francisbaileyh.com/test/test3/index.html?query=x")

        assertEquals(expectedUrl, url.normalizeAndEncode())
    }

    @Test
    fun `does not strip slashes`() {
        val url = URL("https://francisbaileyh.com/test/test3/index.html/")
        val expectedUrl = URL("https://francisbaileyh.com/test/test3/index.html/")

        assertEquals(expectedUrl, url.normalizeAndEncode())
    }

    @Test
    fun `safely handles encoded urls`() {
        val testUrl = URL("https://francisbailey.com/test/test with spaces here.pdf")
        val expectedUrl = URL("https://francisbailey.com/test/test%20with%20spaces%20here.pdf")

        assertEquals(expectedUrl, testUrl.normalizeAndEncode())
    }

    @Test
    fun `returns true if is image type`() {
        val imageTypes = listOf(
            "jpg", "jpeg", "png", "svg", "webp", "gif"
        )

        val nonImageTypes = listOf(
            "php", "js", "aspx"
        )

        imageTypes.forEach {
            val url = URL("https://somesite.com/some-image.$it")
            assertTrue(url.isImagePath())
        }

        imageTypes.forEach {
            val url = URL("https://somesite.com/some-image.${it.uppercase()}")
            assertTrue(url.isImagePath())
        }

        nonImageTypes.forEach {
            val url = URL("https://somesite.com/some-image.${it.uppercase()}")
            assertFalse(url.isImagePath())
        }
    }
}