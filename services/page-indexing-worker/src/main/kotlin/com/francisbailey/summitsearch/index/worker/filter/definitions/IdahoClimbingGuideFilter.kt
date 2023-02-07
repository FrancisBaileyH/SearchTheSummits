package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

// Empty Filter, crawl anything
object IdahoClimbingGuideFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(WordPressFilterChain)
        merge(ImageFilterChain)
    }
}

object IdahoClimbingGuideIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/bookupdates/.*")))
    }
}