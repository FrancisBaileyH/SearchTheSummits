package com.francisbailey.summitsearch.indexservice.extension

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL

class URLExtensionTest {

    @Test
    fun `should return url with last slash removed`() {
        val url = URL("https://francisbaileyh.com/test/test3/index.html/")
        val expectedUrl = URL("https://francisbaileyh.com/test/test3/index.html")

        Assertions.assertEquals(expectedUrl, url.normalizeWithoutSlash())
    }
}