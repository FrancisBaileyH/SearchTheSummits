package com.francisbailey.summitsearch.indexservice

import com.francisbailey.summitsearch.indexservice.common.SimpleQueryString
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SimpleQueryStringTest {

    @Test
    fun `sanitize query produces clean query term with escaped characters`() {
        val query = "Term-with-hyphen *produces| this (query) ~here and tom's 0123456789’"
        val expectedQuery = """Term\-with\-hyphen produces this query here and tom's 0123456789’"""

        val simpleQueryString = SimpleQueryString(phraseBreak = 0, rawQuery = query)

        Assertions.assertEquals(expectedQuery, simpleQueryString.sanitizedQuery())
    }

    @Test
    fun `sanitize query produces clean query split by phrase break`() {
        val query = "Term-with-hyphen *produces| this (query) ~here and tom's 0123456789’"
        val expectedQuery = """"Term\-with\-hyphen produces" "this" "query" "here" "and" "tom's" "0123456789’""""

        val simpleQueryString = SimpleQueryString(phraseBreak = 2, rawQuery = query)

        Assertions.assertEquals(expectedQuery, simpleQueryString.sanitizedQuery())
    }
}