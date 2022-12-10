package com.francisbailey.summitsearch.index.worker.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TaskPermitServiceTest {


    private val permits = 3

    private val permitService = TaskPermitService(permits = permits)

    @Test
    fun `releases permits until none are available`() {
        val key = "test"

        val taskPermits = (1..permits).map {
            permitService.tryAcquirePermit(key)
        }

        assertTrue(taskPermits.all { it != null })
        assertNull(permitService.tryAcquirePermit(key))

        taskPermits.first()?.close()

        assertNotNull(permitService.tryAcquirePermit(key))
    }

    @Test
    fun `releases permits per key`() {
        val usedKey = "test"
        val unusedKey = "test2"

        repeat((0..permits).count()) {
            permitService.tryAcquirePermit(usedKey)
        }

        assertNull(permitService.tryAcquirePermit(usedKey))
        assertNotNull(permitService.tryAcquirePermit(unusedKey))
    }
}