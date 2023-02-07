package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object SVerdinaIndexFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climb\\.asp.*$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*_SHORT\\.asp.*$")))
    }
}