package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object DrDirtbagFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(DefaultFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*[0-9]{1,2}/$"))) // exclude directory style images
    }
}