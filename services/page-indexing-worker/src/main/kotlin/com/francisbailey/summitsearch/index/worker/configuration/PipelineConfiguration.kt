package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.pipeline
import com.francisbailey.summitsearch.index.worker.indexing.step.FetchHtmlPageStep
import com.francisbailey.summitsearch.index.worker.indexing.step.IndexHtmlPageStep
import com.francisbailey.summitsearch.index.worker.indexing.step.SubmitLinksStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PipelineConfiguration(
    private val fetchHtmlPageStep: FetchHtmlPageStep,
    private val submitLinksStep: SubmitLinksStep,
    private val indexHtmlPageStep: IndexHtmlPageStep
) {

    @Bean
    open fun indexingPipeline(): Pipeline {
        return pipeline {
            route(IndexTaskType.HTML) {
                firstRun(fetchHtmlPageStep)
                    .then(submitLinksStep)
                    .then(indexHtmlPageStep)
            }
        }
    }

}