package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.pipeline
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PipelineConfiguration(
    private val fetchHtmlPageStep: FetchHtmlPageStep,
    private val submitLinksStep: SubmitLinksStep,
    private val indexHtmlPageStep: IndexHtmlPageStep,
    private val fetchImageStep: FetchImageStep,
    private val generateThumbnailStep: GenerateThumbnailStep,
    private val saveThumbnailStep: SaveThumbnailStep,
    private val submitThumbnailStep: SubmitThumbnailStep,
    private val thumbnailValidationStep: ThumbnailValidationStep,
    private val contentValidatorStep: ContentValidatorStep,
    private val peakBaggerSubmitThumbnailStep: PeakBaggerSubmitThumbnailStep,
    private val peakBaggerContentValidatorStep: PeakBaggerContentValidatorStep
) {

    @Bean
    open fun indexingPipeline(): Pipeline {
        return pipeline {
            route(IndexTaskType.HTML) {
                firstRun(fetchHtmlPageStep)
                    .then(submitLinksStep)
                    .then(contentValidatorStep)
                        .withHostOverride("peakbagger.com", ContentValidatorStep::class, peakBaggerContentValidatorStep)
                    .then(indexHtmlPageStep)
                    .then(submitThumbnailStep)
                        .withHostOverride("peakbagger.com", SubmitThumbnailStep::class, peakBaggerSubmitThumbnailStep)
            }
            route(IndexTaskType.THUMBNAIL) {
                firstRun(thumbnailValidationStep)
                    .then(fetchImageStep)
                    .then(generateThumbnailStep)
                    .then(saveThumbnailStep)
            }
        }
    }

}