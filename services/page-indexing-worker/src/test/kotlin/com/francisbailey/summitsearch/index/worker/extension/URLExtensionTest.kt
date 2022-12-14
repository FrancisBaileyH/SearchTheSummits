package com.francisbailey.summitsearch.index.worker.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL


class URLExtensionTest {

    @Test
    fun `should return url with fragments removed`() {
        val url = URL("https://francisbaileyh.com/test/test3/index.html?query=x#someFragment")
        val expectedUrl = URL("https://francisbaileyh.com/test/test3/index.html?query=x")

        assertEquals(expectedUrl, url.normalize())
    }

    @Test
    fun `should return url with last slash removed`() {
        val url = URL("https://francisbaileyh.com/test/test3/index.html/")
        val expectedUrl = URL("https://francisbaileyh.com/test/test3/index.html")

        assertEquals(expectedUrl, url.normalize())
    }
}