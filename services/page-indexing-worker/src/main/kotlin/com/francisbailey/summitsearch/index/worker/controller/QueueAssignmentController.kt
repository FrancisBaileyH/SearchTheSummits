package com.francisbailey.summitsearch.index.worker.controller

import com.francisbailey.summitsearch.index.worker.api.*
import com.francisbailey.summitsearch.index.worker.store.QueueAssignmentStore
import mu.KotlinLogging
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.*
import java.net.MalformedURLException


@RestController
class QueueAssignmentController(
    private val queueAssignmentStore: QueueAssignmentStore
) {
    private val log = KotlinLogging.logger { }

    @PutMapping("/api/assignments")
    fun addAssignments(@RequestBody assignmentRequest: PutAssignmentRequest): PutAssignmentResponse {
        log.info { "Received assignment request for urls: ${assignmentRequest.assignments}" }

        try {
            queueAssignmentStore.assign(assignmentRequest.assignments)

            return PutAssignmentResponse(
                status = STATUS.SUCCESS.name,
                assignments = queueAssignmentStore.getAssignments()
            )
        } catch (e: MalformedURLException) {
            log.error(e) { "Bad URL passed in as assignment" }
            throw ServletRequestBindingException("Invalid URL passed as assignment")
        }
    }

    @DeleteMapping("/api/assignments")
    fun clearAssignments(): DeleteAssignmentsResponse {
        log.info { "Clearing all assignments" }
        queueAssignmentStore.clearAssignments()

        return DeleteAssignmentsResponse(
            status = STATUS.SUCCESS.name,
            assignments = queueAssignmentStore.getAssignments()
        )
    }

    @GetMapping("/api/assignments")
    fun getAssignments(): GetAssignmentsResponse {
        return GetAssignmentsResponse(
            assignments = queueAssignmentStore.getAssignments()
        )
    }

    @GetMapping("/api/heartbeat")
    fun getHeartbeat(): GetHeartBeatResponse {
        log.info { "Received heartbeat" }
        return GetHeartBeatResponse(
            status = STATUS.SUCCESS.name
        )
    }

    enum class STATUS {
        SUCCESS,
        FAILURE
    }
}