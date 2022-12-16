package com.francisbailey.summitsearch.indexservice.extension


private val punctuation = setOf("?", ".", "!")

fun String.hasPunctuation(): Boolean {
    return punctuation.contains(this.takeLast(1))
}