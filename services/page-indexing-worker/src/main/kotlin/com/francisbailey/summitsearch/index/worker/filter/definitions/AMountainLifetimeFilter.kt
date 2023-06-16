package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object AMountainLifetimeIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/content/gallery/[A-Za-z0-9-/]{1,500}")))
    }
}