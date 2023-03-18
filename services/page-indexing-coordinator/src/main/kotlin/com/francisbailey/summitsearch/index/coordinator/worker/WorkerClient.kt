package com.francisbailey.summitsearch.index.coordinator.worker

import com.francisbailey.summitsearch.index.coordinator.task.Task
import com.francisbailey.summitsearch.index.worker.api.DeleteAssignmentsResponse
import com.francisbailey.summitsearch.index.worker.api.GetAssignmentsResponse
import com.francisbailey.summitsearch.index.worker.api.PutAssignmentRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.net.URL

@Service
class WorkerClient(
    private val httpClient: HttpClient,
    private val meter: MeterRegistry
) {

    fun getAssignments(worker: Worker): GetAssignmentsResponse {
        val url = URL("${worker.endpoint}/api/assignments")

        return meter.timer("$serviceName.get-assignments.latency", "worker", worker.endpoint).recordCallable {
            runBlocking { httpClient.get(url).body() }
        }!!
    }

    fun clearAssignments(worker: Worker) {
        val url = URL("${worker.endpoint}/api/assignments")

        meter.timer("$serviceName.delete-assignments.latency", "worker", worker.endpoint).recordCallable {
            runBlocking { httpClient.delete(url).body<DeleteAssignmentsResponse>() }
        }
    }

    fun addAssignments(worker: Worker, assignments: List<Task>) {
        val url = URL("${worker.endpoint}/api/assignments")

        meter.timer("$serviceName.put-assignments.latency", "worker", worker.endpoint).recordCallable {
            runBlocking {
                httpClient.put(url) {
                    contentType(ContentType.Application.Json)
                    setBody(PutAssignmentRequest(
                        assignments = assignments.map {
                            it.queueUrl!!
                        }.toSet()
                    ))
                }
            }
        }
    }

    fun sendHeartBeat(worker: Worker) {
        val url = URL("${worker.endpoint}/api/assignments/heartbeat")

        meter.timer("$serviceName.heartbeat.latency", "worker", worker.endpoint).recordCallable {
            runBlocking { httpClient.get(url) }
        }
    }

    companion object {
        const val serviceName = "worker-client"
    }
}


data class Worker(
    val endpoint: String,
    val availableSlots: Int
)