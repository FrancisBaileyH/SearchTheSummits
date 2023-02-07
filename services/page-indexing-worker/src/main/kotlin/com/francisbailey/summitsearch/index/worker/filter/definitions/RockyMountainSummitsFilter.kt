package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern


object RockyMountainSummitsFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/.*/.*\\.htm$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/(?:trip_reports/|)trip_report.php\\?trip_id=[0-9]{1,10}$")))
    }
}

object RockyMountainSummitsIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        merge(RockyMountainSummitsFilter)
    }
}