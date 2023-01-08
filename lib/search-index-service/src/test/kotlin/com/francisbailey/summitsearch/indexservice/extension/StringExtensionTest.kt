package com.francisbailey.summitsearch.indexservice.extension

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StringExtensionTest {

    @Test
    fun `returns correct word count`() {
        val expectations = mapOf(
            "this is  a     test" to 4,
            "hello" to 1,
            "hello     " to 1
        )

        expectations.forEach {
            assertEquals(it.value, it.key.wordCount())
        }
    }

    @Test
    fun `returns expected words`() {
        val expectations = mapOf(
            "this is  a     test" to listOf("this", "is", "a", "test"),
            "hello" to listOf("hello"),
            "hello" to listOf("hello")
        )

        expectations.forEach {
            assertEquals(it.value, it.key.words())
        }
    }

}