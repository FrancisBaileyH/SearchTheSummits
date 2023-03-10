package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern


object ACCVIFilter: DocumentFilterChain(exclusive = false) {

    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/wp-content/bushwhacker/.*")))
    }
}

object ACCVIDiscourseFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/c/trip-report/5\\?page=[0-9]{1,20}")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/c/trip-report/5$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/t/.*")))
    }
}

object ACCVIDiscourseIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/t/.*")))
    }
}