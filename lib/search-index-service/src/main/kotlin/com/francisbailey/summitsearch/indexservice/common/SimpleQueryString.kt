package com.francisbailey.summitsearch.indexservice.common

import com.francisbailey.summitsearch.indexservice.extension.words

class SimpleQueryString(
    private val phraseBreak: Int,
    private val rawQuery: String
) {
    fun sanitizedQuery(): String {
        val term = rawQuery
            .replace(ElasticSearchConstants.QUERY_SANITIZATION_REGEX, "")
            .replace(ElasticSearchConstants.RESERVED_QUERY_REGEX) {
                "\\${it.value}"
            }

        if (phraseBreak == 0) {
            return term
        }

        val words = term.words()
        val sanitizedQuery = StringBuilder()

        sanitizedQuery.append(words.take(phraseBreak).joinToString(prefix = "\"", postfix = "\"", separator = " "))
        sanitizedQuery.append(words.drop(phraseBreak).joinToString(separator = "") { " \"$it\"" })

        return sanitizedQuery.toString()
    }
}