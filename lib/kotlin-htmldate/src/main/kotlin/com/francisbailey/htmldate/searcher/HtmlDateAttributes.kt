package com.francisbailey.htmldate.searcher

class HtmlDateAttributes {

    companion object {
        val DATE_ATTRIBUTES = setOf(
            "article.created",
            "article_date_original",
            "article.published",
            "article:published_time",
            "bt:pubdate",
            "citation_date",
            "citation_publication_date",
            "created",
            "cxenseparse:recs:publishtime",
            "date",
            "date_published",
            "datecreated",
            "dateposted",
            "datepublished",
            // Dublin Core: https://wiki.whatwg.org/wiki/MetaExtensions
            "dc.date",
            "dc.created",
            "dc.date.created",
            "dc.date.issued",
            "dc.date.publication",
            "dcterms.created",
            "dcterms.date",
            "dcterms.issued",
            "dc:created",
            "dc:date",
            "gentime",
            // Open Graph: https://opengraphprotocol.org/
            "og:published_time",
            "og:article:published_time",
            "originalpublicationdate",
            "parsely-pub-date",
            "pubdate",
            "publishdate",
            "publish_date",
            "published-date",
            "publication_date",
            "rnews:datepublished",
            "sailthru.date",
            "shareaholic:article_published_time",
            "timestamp"
        )

        val PROPERTY_MODIFIED_ATTRIBUTES = setOf(
            "article:modified_time",
            "datemodified",
            "modified_time",
            "og:article:modified_time",
            "og:updated_time",
            "og:modified_time",
            "release_date",
            "updated_time"
        )

        val MODIFIED_ATTRIBUTE_KEYS = setOf(
            "lastmodified",
            "last-modified",
            "lastmod"
        )

        val CLASS_ATTRIBUTE_KEYS = setOf(
            "published",
            "date-published",
            "time-published"
        )

        val ITEM_PROPERTY_ORIGINAL = setOf(
            "datecreated",
            "datepublished",
            "pubyear"
        )

        val ITEM_PROPERTY_MODIFIED = setOf(
            "datemodified",
            "dateupdate"
        )

        val ITEM_PROPERTY_ATTRIBUTE_KEYS = ITEM_PROPERTY_ORIGINAL + ITEM_PROPERTY_MODIFIED

        val DATE_CLASS_INDICATORS = setOf(
            "post-meta",
            "post_meta",
            "post__meta",
            "entry-meta",
            "entry-date",
            "article__date",
            "post_detail",
            "meta",
            "meta-before",
            "asset-meta",
            "article-metadata",
            "block-content",
            "byline",
            "dateline",
            "subline",
            "published",
            "posted",
            "submitted",
            "updated",
            "created-post",
            "post-timestamp",
            "date",
            "datum",
            "author",
            "autor",
            "field-content",
            "info",
            "fa-clock-o",
            "fa-calendar",
            "publication",
            "footer",
            "post-footer"
        )

        val DATE_ID_INDICATORS = setOf(
            "article-metadata",
            "post-timestamp",
            "datum",
            "time",
            "post-meta-time",
            "lastmod",
            "metadata",
            "publish",
            "footer"
        )
    }
}