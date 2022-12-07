package com.francisbailey.summitsearch.index.worker.task

import java.net.URL
import java.util.regex.Pattern


class LinkDiscoveryFilterService(
    private val defaultChain: LinkDiscoveryFilterChain
) {
    private val hostFilterChainMap = hashMapOf<String, LinkDiscoveryFilterChain>()

    private val hostFilterCacheChainMap = hashMapOf<String, LinkDiscoveryFilterChain>()

    /**
     * @TODO shouldCache?
     */
    fun shouldFilter(url: URL): Boolean {
        val filterChain = hostFilterChainMap[url.host]

        return filterChain?.shouldFilter(url) ?: defaultChain.shouldFilter(url)
    }

    fun shouldCache(url: URL): Boolean {
        val filterChain = hostFilterCacheChainMap[url.host]

        return filterChain?.shouldFilter(url) ?: false
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
        val fullPath = url.toURI().rawSchemeSpecificPart.replace("//${url.host}", "")
        return fullPath.matches(regex)
    }
}
