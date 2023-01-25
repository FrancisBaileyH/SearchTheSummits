package com.francisbailey.summitsearch.index.worker.indexing

import com.francisbailey.summitsearch.index.worker.client.IndexTask
import com.francisbailey.summitsearch.index.worker.client.IndexTaskDetails
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import org.junit.jupiter.api.Test


class PipelineTest {

    data class Something(
        val something: String
    )

    class TestStringToIntStep: Step<Unit, Int> {
        override fun process(entity: PipelineItem<*>): PipelineItem<Int> {
            println("RUNNING")
            return PipelineItem(entity.task, 12)
        }
    }

    class TestIntToTestStep: Step<Int, Something> {
        override fun process(entity: PipelineItem<*>): PipelineItem<Something> {
            println("Converting int to test")
            return PipelineItem(entity.task, Something("HERE WE ARE ${entity.payload}"))
        }
    }

    @Test
    fun `will break`() {
        val task = IndexTask(
            source = "test",
            details = IndexTaskDetails(
                id = "",
                pageUrl = "",
                taskType = IndexTaskType.THUMBNAIL,
                taskRunId = "",
                refreshIntervalSeconds = 12,
                submitTime = 12
            )
        )

        val pipeline = pipeline {
            route<Any>(IndexTaskType.THUMBNAIL) {
                firstRun(TestStringToIntStep())
                firstRun(TestIntToTestStep())
            }
        }

        pipeline.process(task)
    }

}