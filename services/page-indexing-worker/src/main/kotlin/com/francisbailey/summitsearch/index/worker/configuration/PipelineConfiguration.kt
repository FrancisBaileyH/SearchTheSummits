package com.francisbailey.summitsearch.index.worker.configuration

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.Pipeline
import com.francisbailey.summitsearch.index.worker.indexing.pipeline
import com.francisbailey.summitsearch.index.worker.indexing.step.*
import com.francisbailey.summitsearch.index.worker.indexing.step.override.CascadeClimbersSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerContentValidatorStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.PeakBaggerSubmitThumbnailStep
import com.francisbailey.summitsearch.index.worker.indexing.step.override.SkiSicknessSubmitLinksStep
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
    private val peakBaggerContentValidatorStep: PeakBaggerContentValidatorStep,
    private val fetchPDFStep: FetchPDFStep,
    private val indexPDFStep: IndexPDFStep,
    private val closePDFStep: ClosePDFStep,
    private val generateImagePreviewStep: GenerateImagePreviewStep,
    private val saveImageStep: SaveImageStep,
    private val submitImagesStep: SubmitImagesStep,
    private val checkImageExistsStep: CheckImageExistsStep,
    private val cascadeClimbersSubmitThumbnailStep: CascadeClimbersSubmitThumbnailStep,
    private val skiSicknessSubmitLinksStep: SkiSicknessSubmitLinksStep,
    private val indexFacebookPostStep: IndexFacebookPostStep
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
                    .withHostOverride("skisickness.com", SubmitLinksStep::class, skiSicknessSubmitLinksStep)
                    .withHostOverride("peakbagger.com", SubmitThumbnailStep::class, peakBaggerSubmitThumbnailStep)
                    .withHostOverride("peakbagger.com", ContentValidatorStep::class, peakBaggerContentValidatorStep)
                    .withHostOverride("cascadeclimbers.com", SubmitThumbnailStep::class, cascadeClimbersSubmitThumbnailStep)
                    .withHostOverride("www.facebook.com", IndexHtmlPageStep::class, indexFacebookPostStep)
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