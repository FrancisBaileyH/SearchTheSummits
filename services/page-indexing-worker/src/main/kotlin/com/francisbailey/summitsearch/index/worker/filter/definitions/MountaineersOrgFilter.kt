package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object MountaineersOrgFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-[0-9].*")))
    }
}

object MountaineersOrgIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(MountaineersOrgFilter)
    }
}