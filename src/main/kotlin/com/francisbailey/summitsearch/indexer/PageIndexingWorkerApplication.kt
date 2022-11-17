package com.francisbailey.summitsearch.indexer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class PageIndexingWorkerApplication

fun main(args: Array<String>) {
    runApplication<PageIndexingWorkerApplication>(*args)
}