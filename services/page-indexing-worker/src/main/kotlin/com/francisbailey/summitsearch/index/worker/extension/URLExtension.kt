package com.francisbailey.summitsearch.index.worker.extension

import java.net.URI
import java.net.URL

val IMAGE_EXTENSION_REGEX = Regex(".*(?:png|jpeg|jpg|gif|svg|webp)$", RegexOption.IGNORE_CASE)

/**
 * Strip fragments from URL
 */
fun URL.normalizeAndEncode(): URL {
    val uri = URI(this.toString().replace(" ", "%20")).normalize()
    return URL("${uri.scheme}:${uri.rawSchemeSpecificPart}")
}

fun URL.stripQueryAndFragment(): URL {
    val uri = URI(this.toString().replace(" ", "%20")).normalize()
    return URL("${uri.scheme}://${uri.host}${uri.path}")
}

fun URL.isImagePath(): Boolean {
    return this.path.matches(IMAGE_EXTENSION_REGEX)
}