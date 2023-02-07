package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern


/**
 * NWHikers.net
 *
 * https://www.nwhikers.net/forums/viewforum.php?f=3
 */
object NWHikersFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forums/viewforum\\.php\\?f=3$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forums/viewtopic\\.php\\?t=[0-9]{1,10}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forums/viewforum\\.php\\?f=3&topicdays=0&start=[0-9]{1,10}$")))
    }
}

object NWHikersIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forums/viewtopic\\.php\\?t=[0-9]{1,10}$")))
    }
}