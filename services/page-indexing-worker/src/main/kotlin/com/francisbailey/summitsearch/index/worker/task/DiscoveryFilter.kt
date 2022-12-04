package com.francisbailey.summitsearch.index.worker.crawler

import org.springframework.stereotype.Service
import java.net.URL
import java.util.regex.Pattern

@Service
class LinkDiscoveryFilterService(
    private val defaultChain: LinkDiscoveryFilterChain = DefaultFilterChain
) {
    private val hostFilterChainMap = hashMapOf<String, LinkDiscoveryFilterChain>()

    fun shouldFilter(url: URL): Boolean {
        val filterChain = hostFilterChainMap[url.host]

        return filterChain?.shouldFilter(url) ?: defaultChain.shouldFilter(url)
    }

    fun addFilterChain(url: URL, chain: LinkDiscoveryFilterChain) {
        hostFilterChainMap[url.host] = chain
    }
}

open class LinkDiscoveryFilterChain(
    private val exclusive: Boolean
) {
    private val chain = mutableListOf<DiscoveryFilter>()

    fun addFilter(filter: DiscoveryFilter) {
        chain.add(filter)
    }

    fun shouldFilter(url: URL): Boolean {
        return if (exclusive) {
            chain.any { it.matches(url) }
        } else {
            chain.none { it.matches(url) }
        }
    }
}

interface DiscoveryFilter {
    fun matches(url: URL): Boolean
}

class PathMatchingDiscoveryFilter(
    pattern: Pattern
): DiscoveryFilter {

    private val regex = pattern.toRegex()

    override fun matches(url: URL): Boolean {
        return url.path.matches(regex)
    }
}


object DefaultFilterChain: LinkDiscoveryFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/wp-content/.*" )))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/author/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/tag/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile("^/category/.*")))
        addFilter(PathMatchingDiscoveryFilter(Pattern.compile(".*(?:jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE)))
    }
}