package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object UBCVarsityOutdoorClubFilter: DocumentFilterChain(exclusive = true) {
    init {
        merge(DefaultFilterChain)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/gallery/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/members/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/phorum5/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/wiki.*")))
    }
}