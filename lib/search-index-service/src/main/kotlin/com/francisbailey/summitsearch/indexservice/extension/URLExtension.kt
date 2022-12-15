package com.francisbailey.summitsearch.indexservice.extension

import java.net.URL

fun URL.normalizeWithoutSlash(): URL {
    val uri = this.toURI().normalize()
    return URL("${uri.scheme}:${uri.schemeSpecificPart.removeSuffix("/")}")
}