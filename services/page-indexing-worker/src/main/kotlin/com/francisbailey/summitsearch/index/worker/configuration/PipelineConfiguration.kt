package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.StepOverride
import com.francisbailey.summitsearch.index.worker.indexing.pipeline
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URL

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
    private val fetchPDFStep: FetchPDFStep,
    private val indexPDFStep: IndexPDFStep,
    private val closePDFStep: ClosePDFStep,
    private val generateImagePreviewStep: GenerateImagePreviewStep,
    private val saveImageStep: SaveImageStep,
    private val submitImagesStep: SubmitImagesStep,
    private val checkImageExistsStep: CheckImageExistsStep,
    private val htmlProcessingOverrides: Map<URL, Set<StepOverride<DatedDocument>>>
) {

    @Bean
    open fun indexingPipeline(): Pipeline {
        return pipeline {
            route(IndexTaskType.HTML) {
                firstRun(fetchHtmlPageStep)
                    .then(submitLinksStep)
                    .then(contentValidatorStep)
                    .then(submitImagesStep)
                    .then(indexHtmlPageStep) // destructive step. Need to consider supplying clone
                    .then(submitThumbnailStep)
                    .registerOverrides(htmlProcessingOverrides)
            }
            route(IndexTaskType.THUMBNAIL) {
                firstRun(thumbnailValidationStep)
                    .then(fetchImageStep)
                    .then(generateThumbnailStep)
                    .then(saveThumbnailStep)
            }
            route(IndexTaskType.PDF) {
                firstRun(fetchPDFStep)
                    .then(indexPDFStep)
                    .finally(closePDFStep)
            }
            route(IndexTaskType.IMAGE) {
                firstRun(checkImageExistsStep)
                    .then(fetchImageStep)
                    .then(generateImagePreviewStep)
                    .then(saveImageStep)
            }
        }
    }

}