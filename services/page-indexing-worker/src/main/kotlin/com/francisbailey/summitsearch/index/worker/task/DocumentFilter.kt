package com.francisbailey.summitsearch.index.worker.task

import java.net.URL
import java.util.regex.Pattern


class DocumentFilterService(
    private val defaultChain: DocumentFilterChain
) {
    private val hostFilterChainMap = hashMapOf<String, DocumentFilterChain>()

    fun shouldFilter(url: URL): Boolean {
        val filterChain = hostFilterChainMap[url.host]

        return filterChain?.shouldFilter(url) ?: defaultChain.shouldFilter(url)
    }

    fun addFilterChain(url: URL, chain: DocumentFilterChain) {
        hostFilterChainMap[url.host] = chain
    }
}

open class DocumentFilterChain(
    private val exclusive: Boolean
) {
    private val chain = mutableListOf<DocumentFilter>()

    fun addFilter(filter: DocumentFilter) {
        chain.add(filter)
    }

    fun shouldFilter(url: URL): Boolean {
        return if (exclusive) {
            chain.any { it.matches(url) }
        } else {
            chain.none { it.matches(url) }
        }
    }

    fun merge(filterChain: DocumentFilterChain) {
        this.chain.addAll(filterChain.chain)
    }
}

interface DocumentFilter {
    fun matches(url: URL): Boolean
}

class PathMatchingDocumentFilter(
    pattern: Pattern
): DocumentFilter {

    private val regex = pattern.toRegex()

    override fun matches(url: URL): Boolean {
        val fullPath = url.toURI().rawSchemeSpecificPart.replace("//${url.host}", "")
        return fullPath.matches(regex)
    }
}
