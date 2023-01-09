package com.francisbailey.summitsearch.index.worker.controller

import com.francisbailey.summitsearch.index.worker.api.GetDiscoveriesResponse
import com.francisbailey.summitsearch.index.worker.metadata.PageMetadataStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DiscoveryController(
    private val pageMetadataStore: PageMetadataStore
) {

    @GetMapping("/api/discoveries")
    fun getDiscoveries(): GetDiscoveriesResponse {
        return GetDiscoveriesResponse(
            discoveries = pageMetadataStore.getDiscoveryMetadata().toList().sorted()
        )
    }
}