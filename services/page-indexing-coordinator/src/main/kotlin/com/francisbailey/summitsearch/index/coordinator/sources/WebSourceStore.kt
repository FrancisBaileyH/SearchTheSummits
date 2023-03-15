package com.francisbailey.summitsearch.index.coordinator.sources

import org.springframework.stereotype.Repository

data class IndexSource(
    var host: String? = null,
    var seeds: Set<String>,
    var lastUpdate: Long?,
    var refreshInterval: Long,
    var documentTtl: Long,
    var queueUrl: String,
)

@Repository
class IndexSourceStore {

    fun getRefreshableSources(): List<IndexSource> {
        return emptyList()
    }

    fun save(source: IndexSource) {

    }
}