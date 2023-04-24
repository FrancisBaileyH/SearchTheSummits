package com.francisbailey.summitsearch.index.worker.extractor.strategy

import com.francisbailey.summitsearch.index.worker.extension.getSeoDescription
import com.francisbailey.summitsearch.index.worker.extractor.ContentExtractorStrategy
import com.francisbailey.summitsearch.index.worker.extractor.DocumentText
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.nodes.Document

class WildAirPhotographyContentExtractorStrategy: ContentExtractorStrategy<DocumentText> {

    private val jsonDecoder = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class BlogContentBlock(
        val text: String
    )

    @Serializable
    data class BlogContent(
        val blocks: List<BlogContentBlock>
    )

    @Serializable
    data class BlogDataPost(
        val fullContent: String
    )

    @Serializable
    data class BlogData(
        val post: BlogDataPost
    )

    override fun extract(document: Document): DocumentText {
        val scriptElements = document.select("script")

        val blogDataScript = scriptElements.firstOrNull {
            it.data().startsWith(blogDataPrefix)
        }

        val rawText = if (blogDataScript != null) {
            val blogData = jsonDecoder.decodeFromString<BlogData>(blogDataScript.data()
                .removePrefix(blogDataPrefix)
                .removeSuffix(";")
            )

            val blogContent = jsonDecoder.decodeFromString<BlogContent>(blogData.post.fullContent)

            blogContent.blocks.joinToString(" ") { it.text }
        } else {
            document.body().text()
        }

        return DocumentText(
            title = document.title(),
            description = document.getSeoDescription() ?: "",
            rawText = rawText,
            semanticText = ""
        )
    }


    companion object {
        const val blogDataPrefix = "window._BLOG_DATA="
    }

}