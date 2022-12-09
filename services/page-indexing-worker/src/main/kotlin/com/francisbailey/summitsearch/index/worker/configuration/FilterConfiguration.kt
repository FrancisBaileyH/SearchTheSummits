package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryFilterChain
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryFilterService
import com.francisbailey.summitsearch.index.worker.task.PathMatchingDiscoveryFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL
import java.util.regex.Pattern

@Configuration
open class FilterConfiguration {

    @Bean
    open fun linkDiscoveryFilterService(): LinkDiscoveryFilterService {
        return LinkDiscoveryFilterService(defaultChain = DefaultFilterChain).apply {
            addFilterChain(URL("https://cascadeclimbers.com"), CascadeClimbersFilter)
        }
    }
}

object DefaultFilterChain: LinkDiscoveryFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/.*[?].*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/wp-content/.*" )))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/author/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/tag/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/category/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile(".*(?:jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE)))
    }
}

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
object CascadeClimbersFilter: LinkDiscoveryFilterChain(exclusive = false) {
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
            addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/forum/forum/$it(?:/|/\\?page=[1-9]{1,10})$")))
        }
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("/forum/topic/[0-9]{1,7}-tr-[a-z0-9-]{1,250}/$")))
    }

}