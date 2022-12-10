package com.francisbailey.summitsearch.index.worker.task

import java.util.concurrent.Semaphore


class TaskPermitService(
    val permits: Int
) {
    private val permitMap = hashMapOf<String, Semaphore>()

    fun tryAcquirePermit(key: String): TaskPermit? = synchronized(this) {
        val permit = permitMap.getOrPut(key) {
            Semaphore(permits)
        }

        return if (permit.tryAcquire()) {
            TaskPermit(permit)
        } else {
            null
        }
    }
}


class TaskPermit(
    private val semaphore: Semaphore
): AutoCloseable {
    override fun close() {
        this.semaphore.release()
    }
}