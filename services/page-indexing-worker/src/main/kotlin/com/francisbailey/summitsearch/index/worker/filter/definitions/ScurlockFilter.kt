package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object ScurlockFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/nolock/image/[0-9]{3,10}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/nolock/image/[0-9]{3,10}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/nolock/[a-zA-Z0-9-_]{1,200}$")))
    }
}

/**
 * This is an image gallery and we don't actually need to index any of the pages since
 * they contain so few details. Instead we'll just submit the images for processing with the associated captions.
 */
object ScurlockIndexFilter: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
    }
}