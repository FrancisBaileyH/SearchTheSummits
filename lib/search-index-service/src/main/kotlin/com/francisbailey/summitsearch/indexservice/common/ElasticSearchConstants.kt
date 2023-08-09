package com.francisbailey.summitsearch.indexservice.common

internal class ElasticSearchConstants {
    companion object {
        const val HIGHLIGHT_DELIMITER = "<em>"
        const val SORT_DATE_FORMAT = "strict_date_optional_time_nanos"
        const val SORT_LAST_NAME = "_last"

        val RESERVED_QUERY_REGEX = Regex("[-+|*()~]")
        val QUERY_SANITIZATION_REGEX = Regex("[^-a-zA-Z0-9’'\"\\s]")
    }
}