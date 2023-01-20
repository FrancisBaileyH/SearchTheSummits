package com.francisbailey.summitsearch.indexservice.extension

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URL

class ElasticsearchClientExtensionTest {

    @Test
    fun `generate ID produces normalized ID from URL`() {
        val expectations = mapOf(
            "https://francisbailey.com" to "francisbailey.com",
            "http://francisbailey.com/path/to/" to "francisbailey.com/path/to",
            "http://francisbailey.com/path/to/?paramB=x&paramA=y" to "francisbailey.com/path/to?paramA=y&paramB=x",
            "https://francisbailey.com/path/to/index.html" to "francisbailey.com/path/to/index.html",
            "https://francisbailey.com/path/to/index.html?query=x&alpha=g" to "francisbailey.com/path/to/index.html?alpha=g&query=x",
            "http://subdomain.francisbailey.com" to "subdomain.francisbailey.com"
        )

        expectations.forEach {
            Assertions.assertEquals(it.value, generateIdFromUrl(URL(it.key)))
        }
    }
}