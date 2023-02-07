package com.francisbailey.summitsearch.index.worker.filter.definitions


import com.francisbailey.summitsearch.index.worker.filter.DocumentFilterChain
import com.francisbailey.summitsearch.index.worker.filter.PathMatchingDocumentFilter
import java.util.regex.Pattern

object WordPressFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*/wp-content/.*" )))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*/author/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*/tag/.*")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*/category/.*")))
    }
}

object BlogSpotFilterchain: DocumentFilterChain(exclusive = true) {
    init {
        // Exclude Blogspot archives e.g. /2022 or /2022/12 or /2022/10/12
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*/(?:[0-9]{4}|[0-9]{4}/[0-9]{2}|[0-9]{4}/[0-9]{2}/[0-9]{2})(?:/|)\$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/search/.*$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^/feeds/.*$")))
    }
}

object ImageFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*(?:jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE)))
    }
}

object DefaultFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        // Exclude Blogspot archives e.g. /2022 or /2022/12 or /2022/10/12
        merge(BlogSpotFilterchain)
        // Exclude query parameters by default
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*[?].*")))
        // Wordpress filters
        merge(WordPressFilterChain)
        // Exclude images
        merge(ImageFilterChain)
        // Exclude ecommerce links (best effort)
        addFilter(PathMatchingDocumentFilter(Pattern.compile(".*/product/.*|.*/store/.*")))
    }
}

object DefaultIndexFilterChain: DocumentFilterChain(exclusive = true) {
    init {
        // Skip indexing home pages as they're typically filled will previews or full feeds of the article
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^(?:/|/index.html|)$")))
        addFilter(PathMatchingDocumentFilter(Pattern.compile("^.*page/[0-9]{1,5}(?:/|)$")))
    }
}
