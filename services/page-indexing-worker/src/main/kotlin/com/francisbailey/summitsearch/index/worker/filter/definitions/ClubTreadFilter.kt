package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object ClubTreadFilter: DocumentFilterChain(exclusive = false) {
    init {
        val allowedTopics = listOf(
            "27-british-columbia",
            "130-canadian-rockies",
            "34-alberta",
            "37-washington-state",
            "35-other-regions"
        )

        allowedTopics.forEach {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/$it(?:|/|/index[0-9]{1,10}.html)$")))
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/$it/[0-9]{1,7}[a-z0-9-]{1,250}[a-z](?<!print|prev-thread|next-thread).html$")))
        }
    }
}