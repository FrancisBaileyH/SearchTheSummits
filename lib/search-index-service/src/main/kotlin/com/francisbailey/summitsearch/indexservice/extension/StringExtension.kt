package com.francisbailey.summitsearch.indexservice.extension

import java.util.regex.Pattern

val String.Companion.wordDelimiterPattern: Pattern
    get() = Pattern.compile("\\s+")

fun String.words(): List<String> = this.trim().split(regex = String.wordDelimiterPattern)

fun String.wordCount(): Int = this.words().size