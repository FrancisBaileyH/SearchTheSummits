package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.worker.api.GetAssignmentsResponse
import org.springframework.stereotype.Service

@Service
class WorkerClient {

    fun getAssignments(workerEndpoint: Worker): GetAssignmentsResponse {
        return GetAssignmentsResponse(assignments = emptySet())
    }

    fun clearAssignments(workerEndpoint: Worker) {

    }

    fun addAssignments(workerEndpoint: Worker, assignments: List<Task>) {

    }
}

data class WorkerEndpoint(
    val endpoint: String
)

data class Worker(
    val workerEndpoint: WorkerEndpoint,
    var availableSlots: Int
)