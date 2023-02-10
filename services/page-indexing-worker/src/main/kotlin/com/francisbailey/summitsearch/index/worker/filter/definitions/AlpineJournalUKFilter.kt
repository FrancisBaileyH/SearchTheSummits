package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern


object AlpineJournalUKFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(ImageFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/cgi-bin/.*")))
    }
}