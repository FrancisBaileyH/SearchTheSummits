package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object AlpineBennyFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(DefaultFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/blog(?:|/.*)$")))
    }
}