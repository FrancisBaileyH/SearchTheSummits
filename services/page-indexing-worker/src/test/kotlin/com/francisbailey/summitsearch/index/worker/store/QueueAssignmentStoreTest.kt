package com.francisbailey.summitsearch.index.worker.store

import com.francisbailey.summitsearch.index.worker.client.IndexingTaskQueueClient
import com.francisbailey.summitsearch.services.common.MonotonicClock
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

class QueueAssignmentStoreTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val indexingTaskQueueClient = mock<IndexingTaskQueueClient>()
    private val clock = mock<MonotonicClock>()
    private val timeMark = mock<MonotonicClock.TimeMark>()

    private val store = QueueAssignmentStore(
        meterRegistry,
        indexingTaskQueueClient,
        clock
    )

    @Test
    fun `clears assignments after 10 seconds of no refresh`() {
        val assignments = setOf("abc123")
        whenever(clock.now()).thenReturn(timeMark)
        whenever(clock.timeSince(any())).thenReturn(11.seconds)

        store.assign(assignments)
        assertEquals(assignments, store.getAssignments())

        store.updateAssignmentKeepAliveState()
        store.monitorAssignmentKeepAliveState()

        assertTrue(store.getAssignments().isEmpty())
    }

    @Test
    fun `keeps assignments as long as there is a refresh in the last 10 seconds`() {
        val assignments = setOf("abc123")
        whenever(clock.now()).thenReturn(timeMark)
        whenever(clock.timeSince(any())).thenReturn(10.seconds)

        store.assign(assignments)
        store.updateAssignmentKeepAliveState()
        store.monitorAssignmentKeepAliveState()

        assertEquals(assignments, store.getAssignments())
    }
}