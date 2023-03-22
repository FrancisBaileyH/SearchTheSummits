package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object SuperTopoFilter: DocumentFilterChain(exclusive = false) {

    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/tr/.*n.html")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climbing/forum_trip_reports\\.php\\?s=view.*")))
    }
}

object SuperTopoIndexFilter: DocumentFilterChain(exclusive = false) {
    // http://www.supertopo.com/climbing/forum_trip_reports.php?s=views&o=DESC&v=1&cur=20&ftr=#list
    // http://www.supertopo.com/tr/West-Face-of-Starr-King-Hidden-Gem-of-Yosemite/t10616n.html
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/tr/.*n.html")))
    }
}