package com.francisbailey.summitsearch.index.worker.configuration

import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PDFConfiguration {

    @Bean
    open fun pdfTextStripper() = PDFTextStripper()

    @Bean
    open fun pdfPagePartitionThreshold(): Int = 10
}