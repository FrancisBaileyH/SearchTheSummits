package com.francisbailey.summitsearch.index.worker.filter

import com.francisbailey.summitsearch.index.worker.task.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.task.PathMatchingDocumentFilter
import java.util.regex.Pattern


object PeakBaggerFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/range\\.aspx.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climber/PeakAscents.aspx?pid=.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climber/ascent\\.aspx\\?aid=[0-9]{1,100}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climber/climber\\.aspx\\?cid=[0-9]{1,100}$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climber/ClimbListC\\.aspx\\?cid=[0-9]{1,100}&j=0$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/peak\\.aspx\\?pid=.*$", Pattern.CASE_INSENSITIVE)))
    }
}

object PeakBaggerIndexFilter: DocumentFilterChain(exclusive = false) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/climber/ascent\\.aspx\\?aid=[0-9]{1,100}$")))
    }
}