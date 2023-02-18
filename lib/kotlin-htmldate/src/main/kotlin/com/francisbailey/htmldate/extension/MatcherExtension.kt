package com.francisbailey.htmldate.extension

import java.util.regex.Matcher

fun Matcher.intParts(upTo: Int? = null): IntArray {
    val upperBound = upTo ?: groupCount()

    return (1..upperBound).map {
        this.group(it).toInt()
    }.toIntArray()
}

fun Matcher.hasGroupCount(count: Int): Boolean {
    return find() && groupCount() >= count
}