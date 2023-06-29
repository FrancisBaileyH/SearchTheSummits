package com.francisbailey.summitsearch.indexservice.common


interface TextNormalizer {
    fun normalize(text: String): String
}

class DefaultTextNormalizer: TextNormalizer {
    override fun normalize(text: String): String {
        return APOSTROPHE_REGEX.replace(text, DEFAULT_APOSTROPHE)
    }

    companion object {
        val APOSTROPHE_REPRESENTATIONS = listOf(
            "\u0091",
            "\u0092",
            "\u2018",
            "\u2019",
            "\uFF07",
            "\u02BB"
        )

        val APOSTROPHE_REGEX = Regex("[${APOSTROPHE_REPRESENTATIONS.joinToString("")}]")

        val DEFAULT_APOSTROPHE = "\u0027"
    }
}