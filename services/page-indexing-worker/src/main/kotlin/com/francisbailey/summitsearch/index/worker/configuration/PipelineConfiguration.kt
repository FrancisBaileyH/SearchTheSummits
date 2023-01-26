package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.pipeline
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PipelineConfiguration(
    private val fetchHtmlPageStep: FetchHtmlPageStep,
    private val submitLinksStep: SubmitLinksStep,
    private val indexHtmlPageStep: IndexHtmlPageStep,
    private val fetchImageStep: FetchImageStep,
    private val generateThumbnailStep: GenerateThumbnailStep,
    private val saveThumbnailStep: SaveThumbnailStep
) {

    @Bean
    open fun indexingPipeline(): Pipeline {
        return pipeline {
            route(IndexTaskType.HTML) {
                firstRun(fetchHtmlPageStep)
                    .then(submitLinksStep)
                    .then(indexHtmlPageStep)
            }
            route(IndexTaskType.THUMBNAIL) {
                firstRun(fetchImageStep)
                    .then(generateThumbnailStep)
                    .then(saveThumbnailStep)
            }
        }
    }

}