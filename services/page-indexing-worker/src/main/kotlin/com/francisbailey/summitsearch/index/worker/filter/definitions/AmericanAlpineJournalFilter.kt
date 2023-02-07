package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object AmericanAlpineJournalIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles/[0-9]{1,20}.?$")))
    }
}

object AmericanAlpineJournalFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles/[0-9]{1,20}.?$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/articles\\?page=[0-9]{1,20}$")))
    }
}