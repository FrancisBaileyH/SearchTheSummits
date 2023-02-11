package com.francisbailey.summitsearch.index.worker.extension

import org.junit.jupiter.api.Assertions.assertEquals
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
}