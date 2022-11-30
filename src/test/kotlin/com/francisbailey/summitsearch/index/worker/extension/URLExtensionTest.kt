package com.francisbailey.summitsearch.index.worker.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL


class URLExtensionTest {

    @Test
    fun `should return url with fragments removed`() {
        val url = URL("https://francisbaileyh.com/test?query=x#someFragment")
        val expectedUrl = URL("https://francisbaileyh.com/test?query=x")

        assertEquals(expectedUrl, url.normalize())
    }
}