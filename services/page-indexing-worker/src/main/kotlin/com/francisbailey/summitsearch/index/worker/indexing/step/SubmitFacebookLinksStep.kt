package com.francisbailey.summitsearch.index.worker.indexing.step

import com.francisbailey.summitsearch.index.worker.client.IndexTaskType
import com.francisbailey.summitsearch.index.worker.indexing.PipelineItem
import com.francisbailey.summitsearch.index.worker.indexing.PipelineMonitor
import com.francisbailey.summitsearch.index.worker.indexing.Step
import com.francisbailey.summitsearch.index.worker.task.Discovery
import com.francisbailey.summitsearch.index.worker.task.LinkDiscoveryService
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class SubmitFacebookLinksStep(
    private val linkDiscoveryService: LinkDiscoveryService
): Step<DatedDocument> {
    override fun process(entity: PipelineItem<DatedDocument>, monitor: PipelineMonitor): PipelineItem<DatedDocument> {
        val postUrlMatches = FACEBOOK_POST_PATTERN.matcher(entity.payload!!.document.html())

        val postUrlDiscoveries = postUrlMatches.results().map {
            Discovery(
                source = it.group().replace("\\", ""),
                type = IndexTaskType.HTML
            )
        }.toList()

        linkDiscoveryService.submitDiscoveries(entity.task, postUrlDiscoveries)

        return entity.apply { continueProcessing = true }
    }

    companion object {
        private val FACEBOOK_POST_PATTERN = Pattern.compile("(https:\\\\/\\\\/www.facebook.com\\\\/groups\\\\/[0-9]{1,20}\\\\/posts\\\\/[0-9]{1,20}\\\\/)")
    }
}