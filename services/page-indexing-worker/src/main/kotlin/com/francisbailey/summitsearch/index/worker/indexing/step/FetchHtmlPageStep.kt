package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.htmldate.GoodEnoughHtmlDateGuesser
import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.crawler.*
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import com.francisbailey.summitsearch.indexservice.SummitSearchDeleteIndexRequest
import com.francisbailey.summitsearch.indexservice.SummitSearchIndexService
import io.ktor.http.*
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component
import java.lang.Exception
import java.time.LocalDateTime

data class DatedDocument(
    val pageCreationDate: LocalDateTime? = null,
    val document: Document
)

@Component
class FetchHtmlPageStep(
    private val pageCrawlerService: PageCrawlerService,
    private val linkDiscoveryService: LinkDiscoveryService,
    private val indexService: SummitSearchIndexService,
    private val htmlDateGuesser: GoodEnoughHtmlDateGuesser
): Step<DatedDocument> {

    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        return monitor.meter.timer("page.latency", "host", entity.task.details.entityUrl.host).recordCallable {
            monitor.sourceCircuitBreaker.executeCallable {
                getDocument(entity, monitor)
            }
        }!!
    }

    private fun getDocument(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val host = entity.task.details.entityUrl.host

        return try {
            val document = pageCrawlerService.get(entity.task.details.entityUrl)
            val date = monitor.meter.timer("dateguess.latency", "host", host).recordCallable {
                htmlDateGuesser.findDate(entity.task.details.entityUrl, document)
            }

            if (date == null) {
                monitor.meter.counter("dateguess.miss", "host", host).increment()
            }

            entity.apply {
                payload = DatedDocument(
                    pageCreationDate = date,
                    document = document
                )
                continueProcessing = true
            }
        } catch (e: Exception) {
            when (e) {
                is RedirectedEntityException -> {
                    e.location?.run {
                        monitor.meter.counter("page.redirects").increment()
                        linkDiscoveryService.submitDiscoveries(entity.task, listOf(Discovery(IndexTaskType.HTML, this)))
                    }
                }
                is UnsupportedEntityException -> {
                    e.contentType?.run {
                        if (this.match(ContentType.Application.Pdf)) {
                            log.info { "Found PDF on HTML route. Re-routing." }
                            linkDiscoveryService.submitDiscoveries(
                                entity.task,
                                listOf(Discovery(
                                    type = IndexTaskType.PDF,
                                    source = entity.task.details.entityUrl.toString(),
                                    skipCacheCheck = true
                                )),
                            )
                        }
                    }
                }
                is NonRetryableEntityException -> {
                    monitor.dependencyCircuitBreaker.executeCallable {
                        indexService.deletePageContents(SummitSearchDeleteIndexRequest(entity.task.details.entityUrl))
                        monitor.meter.counter("indexservice.delete").increment()
                        log.error(e) { "Unable to index page: ${entity.task.details.entityUrl}" }
                    }
                }
            }

            throw e
        }
    }
}