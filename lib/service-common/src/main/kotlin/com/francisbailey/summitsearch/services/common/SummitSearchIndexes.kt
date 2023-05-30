package com.francisbailey.summitsearch.services.common

class SummitSearchIndexes {
    companion object {
        val synonyms = listOf(
            "mt., mt, mount",
            "mtn, mtn., mountain",
            "se, south east, southeast",
            "sw, south west, southwest",
            "nw, north west, northwest",
            "ne, north east, northeast",
            "bc, british columbia",
            "fsr, forest service road, service road",
            "rd, road",
            "pk, peak"
        )

        const val documentIndexName = "summit-search-index"
        const val imageIndexName = "summit-search-images"
        const val placeNameIndex = "summit-locations-index"
    }
}