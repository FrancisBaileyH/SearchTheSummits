package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.extension.getSeoDescription
import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterService
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.indexservice.DocumentIndexService
import com.francisbailey.summitsearch.indexservice.DocumentPutRequest
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import org.springframework.stereotype.Component
import javax.swing.text.html.HTML

@Component
class IndexHtmlPageStep(
    private val documentIndexingFilterService: DocumentFilterService,
    private val documentIndexService: DocumentIndexService
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        if (documentIndexingFilterService.shouldFilter(entity.task.details.entityUrl)) {
            log.warn { "Skipping indexing on: ${entity.task.details.entityUrl} as it matches filter" }
            return entity
        }

        val document = entity.payload!!.document

        document.body().select(EXCLUDED_TAG_EVALUATOR).forEach {
            it.remove()
        }

        val title = document.title().ifBlank {
            entity.task.details.entityUrl.host
        }

        val textOnly = document.body().text()
        val paragraphContent = document.body().select(HTML.Tag.P.toString()).text()
        val description = document.getSeoDescription() ?: ""

        monitor.meter.timer("indexservice.add.latency").recordCallable {
            monitor.dependencyCircuitBreaker.executeCallable {
                documentIndexService.indexContent(
                    DocumentPutRequest(
                        source = entity.task.details.entityUrl,
                        title = title,
                        seoDescription = description,
                        paragraphContent = paragraphContent,
                        rawTextContent = textOnly,
                        pageCreationDate = entity.payload!!.pageCreationDate
                    )
                )
            }
        }

        log.info { "Successfully completed indexing task for: ${entity.task.source} with ${entity.task.details.entityUrl}" }
        monitor.meter.counter("indexservice.add.success", "host" , entity.task.details.entityUrl.host).increment()

        return entity.apply { continueProcessing = true }
    }

    companion object {
        private val EXCLUDED_TAG_EVALUATOR = object: Evaluator() {
            private val excludedTags = setOf("ul", "li", "a", "nav", "footer", "header")

            override fun matches(root: Element, element: Element): Boolean {
                return excludedTags.contains(element.normalName())
            }
        }
    }
}