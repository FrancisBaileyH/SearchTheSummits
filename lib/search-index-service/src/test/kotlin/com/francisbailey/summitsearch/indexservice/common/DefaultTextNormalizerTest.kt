package com.francisbailey.summitsearch.indexservice.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefaultTextNormalizerTest {

    private val normalizer = DefaultTextNormalizer()
    private val APOSTROPHE_REPRESENTATIONS = listOf(
        "\u0091",
        "\u0092",
        "\u2018",
        "\u2019",
        "\uFF07",
        "\u02BB"
    )

    @Test
    fun `replaces non-standard apostrophes with standard apostrophe`() {
        APOSTROPHE_REPRESENTATIONS.forEach {
            assertEquals("Shark\u0027s Fin", normalizer.normalize("Shark${it}s Fin"))
        }
    }

    @Test
    fun `replaces non-standard apostrophes in multi-line context with standard apostrophe`() {
        APOSTROPHE_REPRESENTATIONS.forEach {
            assertEquals("Shark\u0027s Fin \r\n and Tom\u0027s Bike", normalizer.normalize("Shark${it}s Fin \r\n and Tom${it}s Bike"))
        }
    }
}