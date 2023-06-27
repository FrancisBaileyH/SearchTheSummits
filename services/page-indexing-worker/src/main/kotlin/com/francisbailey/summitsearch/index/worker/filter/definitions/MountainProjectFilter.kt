package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object MountainProjectFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(MountainProjectIndexFilter)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/area/[0-9]{1,12}/[a-zA-Z0-9-]{1,200}$")))
    }
}

object MountainProjectIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/route/[0-9]{1,12}/[a-zA-Z0-9-]{1,200}$")))
    }
}