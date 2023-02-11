package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLDecoder


@Component
class IndexPDFStep(
    private val textStripper: () -> PDFTextStripper,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<PDDocument> {

    @Autowired
    constructor(
        summitSearchIndexService: SummitSearchIndexService
    ): this({ PDFTextStripper() }, summitSearchIndexService)

    override fun process(entity: PipelineItem<PDDocument>, monitor: PipelineMonitor): PipelineItem<PDDocument> {

        entity.payload?.let {
            val textContent = textStripper().getText(it)
            val condensedContent = textContent.replace("\r\n", " ")
            val pdfPath = entity.task.details.pageUrl.path

            monitor.meter.timer("$metricPrefix.indexservice.add.latency", "host", entity.task.details.pageUrl.host).recordCallable {
                monitor.dependencyCircuitBreaker.executeCallable {
                    summitSearchIndexService.indexPageContents(
                        request = SummitSearchIndexRequest(
                            source = entity.task.details.pageUrl,
                            rawTextContent = condensedContent,
                            paragraphContent = "",
                            seoDescription = "",
                            title = pdfPath
                                .substringAfterLast("/")
                                .substringBeforeLast(".pdf")
                                .run { URLDecoder.decode(this, Charsets.UTF_8.name()) }
                        )
                    )
                }
            }!!
        }

        return entity
    }
}