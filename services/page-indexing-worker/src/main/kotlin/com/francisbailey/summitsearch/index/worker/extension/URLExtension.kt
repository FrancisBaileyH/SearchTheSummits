package com.francisbailey.summitsearch.index.worker.extension

import java.net.URI
import java.net.URL

/**
 * Strip fragments from URL
 */
fun URL.normalizeAndEncode(): URL {
    val uri = URI(this.toString().replace(" ", "%20")).normalize()
    return URL("${uri.scheme}:${uri.rawSchemeSpecificPart}")
}