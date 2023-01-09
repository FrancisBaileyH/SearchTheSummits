package com.francisbailey.summitsearch.index.worker.api

import kotlinx.serialization.Serializable

@Serializable
data class GetDiscoveriesResponse(
    val discoveries: List<String>
)