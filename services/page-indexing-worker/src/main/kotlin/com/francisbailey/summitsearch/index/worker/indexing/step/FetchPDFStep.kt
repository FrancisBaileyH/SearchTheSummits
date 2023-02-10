package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.crawler.PDFCrawlerService
import com.francisbailey.summitsearch.index.worker.crawler.RetryableEntityException
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class FetchPDFStep(
    private val pdfCrawlerService: PDFCrawlerService
): Step<PDDocument> {
    override fun process(entity: PipelineItem<PDDocument>, monitor: PipelineMonitor): PipelineItem<PDDocument> {
        return monitor.meter.timer("$metricPrefix.latency.pdf", "host", entity.task.details.pageUrl.host).recordCallable {
            monitor.sourceCircuitBreaker.executeCallable {
                try {
                    val document = pdfCrawlerService.get(entity.task.details.pageUrl)
                    entity.apply { payload = document }
                } catch (e: Exception) {
                    if (e is RetryableEntityException) {
                        entity.canRetry = true
                    }
                    log.error(e) { "Failed to fetch PDF from: ${entity.task.details.pageUrl}" }
                    entity.apply { continueProcessing = false }
                }
            }
        }!!
    }
}