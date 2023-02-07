package com.francisbailey.summitsearch.index.worker.filter.definitions

import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

/**
 * Allowed:
 * /forum/forum/16-british-columbiacanada/?page=4
 * /forum/forum/16-british-columbiacanada/
 * /forum/topic/99505-tr-angle-of-the-dangle/
 *
 * Not Allowed:
 * https://cascadeclimbers.com/forum/topic/75809-tr-joffre-peak-flavelle-lane-8152010/?do=getLastComment
 * /forum/forum/16-british-columbiacanada/?sortby=title&sortdirection=asc&page=4
 */
object CascadeClimbersFilter: DocumentFilterChain(exclusive = false) {
    init {
        val allowedTopics = listOf(
            "34-alaska",
            "16-british-columbiacanada",
            "10-north-cascades",
            "11-alpine-lakes",
            "13-southern-wa-cascades",
            "12-mount-rainier-np",
            "14-olympic-peninsula",
            "28-centraleastern-washington",
            "15-oregon-cascades",
            "41-columbia-river-gorge",
            "25-california",
            "49-idaho",
            "48-montana",
            "52-the-rest-of-the-us-and-international"
        )
        allowedTopics.forEach {
            addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forum/forum/$it(?:|/|/page/[0-9]{1,10}/?|)$")))
        }
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/forum/topic/[0-9]{1,7}-tr-[a-z0-9-]{1,250}(?:|/)$")))
    }
}