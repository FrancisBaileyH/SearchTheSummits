package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object BCMCFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/m/docs/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/media/newsletters/.*")))
    }
}