package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import com.francisbailey.summitsearch.indexservice.SummitSearchPutRequest
import org.apache.pdfbox.pdmodel.PDDocument
import org.springframework.stereotype.Component
import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.beans.factory.annotation.Autowired
import java.net.URL
import java.net.URLDecoder


@Component
class IndexPDFStep(
    private val pdfPagePartitionThreshold: Int,
    private val textStripper: () -> PDFTextStripper,
    private val summitSearchIndexService: SummitSearchIndexService
): Step<PDDocument> {

    @Autowired
    constructor(
        summitSearchIndexService: SummitSearchIndexService,
        pdfPagePartitionThreshold: Int
    ): this(
        pdfPagePartitionThreshold,
        { PDFTextStripper() },
        summitSearchIndexService
    )

    override fun process(entity: PipelineItem<PDDocument>, monitor: PipelineMonitor): PipelineItem<PDDocument> {
        entity.payload?.let {
            val textStripper = textStripper()
            val pageRange = (1..it.numberOfPages)

            log.info { "Indexing ${entity.task.details.entityUrl}" }

            if (it.numberOfPages <= pdfPagePartitionThreshold) {
                monitor.meter.timer("indexservice.add.latency", "host", entity.task.details.entityUrl.host).recordCallable {
                    monitor.dependencyCircuitBreaker.executeCallable {
                        summitSearchIndexService.indexContent(buildRequest(
                            textStripper.getText(it),
                            entity.task.details.entityUrl
                        ))
                    }
                }
            } else {
                log.info { "Partition threshold reached. Partitioning PDF document: ${entity.task.details.entityUrl}" }
                val requests = pageRange.chunked(pdfPagePartitionThreshold).map { pagePartition ->
                    textStripper.apply {
                        startPage = pagePartition.first()
                        endPage = pagePartition.last()
                    }

                    val textContent = textStripper.getText(it)
                    val suffix = when {
                        pagePartition.size > 2 -> " (pages ${pagePartition.first()}-${pagePartition.last()})"
                        pagePartition.size > 1 -> " (pages ${pagePartition.first()},${pagePartition.last()})"
                        else -> " (page ${pagePartition.first()})"

                    }

                    val anchoredPdf = URL("${entity.task.details.entityUrl}#page=${pagePartition.first()}")

                    buildRequest(textContent, anchoredPdf, suffix)
                }

                requests.chunked(summitSearchIndexService.maxBulkIndexRequests).forEach {
                    monitor.meter.timer("indexservice.bulk-add.latency", "host", entity.task.details.entityUrl.host).recordCallable {
                        monitor.dependencyCircuitBreaker.executeCallable {
                            summitSearchIndexService.indexPartitionedContent(it)
                        }
                    }
                }
            }
        }

        return entity.apply { continueProcessing = true }
    }

    private fun buildRequest(textContent: String, pdfUrl: URL, titleSuffix: String? = ""): SummitSearchIndexRequest {
        val condensedContent = textContent.replace("\r\n", " ")

        return SummitSearchPutRequest(
            source = pdfUrl,
            rawTextContent = condensedContent,
            paragraphContent = "",
            seoDescription = "",
            title = pdfUrl.path
                .substringAfterLast("/")
                .substringBeforeLast(".pdf")
                .run { URLDecoder.decode(this, Charsets.UTF_8.name()) }
                .run { "$this$titleSuffix" }
        )
    }
}