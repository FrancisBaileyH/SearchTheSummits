package com.francisbailey.summitsearch.index.worker.extension

import java.net.URL

/**
 * Strip fragments from URL
 */
fun URL.normalize(): URL {
    val uri = this.toURI()
    return URL("${uri.scheme}:${uri.schemeSpecificPart}")
}