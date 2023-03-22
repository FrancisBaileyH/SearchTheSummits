package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object SkiSicknessFilter: DocumentFilterChain(exclusive = false) {

    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/post/viewforum\\.php\\?f=3$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/post/viewforum\\.php\\?f=3&start=[0-9]{1,10}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/post/v[a-zA-Z0-9-]{1,400}")))
    }
}

object SkiSicknessIndexFilter: DocumentFilterChain(exclusive = false) {

    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/post/v[a-zA-Z0-9-]{1,400}")))
    }
}