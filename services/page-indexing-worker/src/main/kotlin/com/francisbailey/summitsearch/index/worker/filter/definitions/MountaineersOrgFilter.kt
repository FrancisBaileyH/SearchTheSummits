package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object MountaineersOrgFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(MountaineersOrgIndexFilter)
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/activities/trip-reports/@@faceted_query\\?b_start:int=[0-9]{1,20}$")))
    }
}

object MountaineersOrgIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/about/history/the-mountaineer-annuals/indexes-annuals-maps/the-mountaineer-[0-9].*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/activities/trip-reports/[a-zA-Z0-9-]{1,250}")))
    }
}