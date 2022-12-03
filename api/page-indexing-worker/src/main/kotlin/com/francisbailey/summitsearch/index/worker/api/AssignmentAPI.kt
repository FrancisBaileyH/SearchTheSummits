package com.francisbailey.summitsearch.index.worker.api

import kotlinx.serialization.Serializable


@Serializable
data class PutAssignmentRequest(
    val assignments: Set<String>
)

@Serializable
data class PutAssignmentResponse(
    val status: String
)

@Serializable
data class DeleteAssignmentsResponse(
    val status: String
)

@Serializable
data class GetAssignmentsResponse(
    val assignments: Set<String>
)