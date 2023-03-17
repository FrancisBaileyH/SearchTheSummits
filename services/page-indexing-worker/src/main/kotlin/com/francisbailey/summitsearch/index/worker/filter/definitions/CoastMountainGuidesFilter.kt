package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object CoastMountainGuidesFilter: DocumentFilterChain(exclusive = false) {
    init {
        AltusMountainGuidesFilter.addFilter(PathMatchingDocumentFilter(Pattern.compile(".*")))
    }
}