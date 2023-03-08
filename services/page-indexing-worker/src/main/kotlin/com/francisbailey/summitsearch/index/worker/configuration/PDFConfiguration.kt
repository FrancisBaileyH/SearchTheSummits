package com.francisbailey.summitsearch.index.worker.configuration

import org.apache.pdfbox.text.PDFTextStripper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
open class PDFConfiguration {

    @Bean
    open fun pdfTextStripper() = PDFTextStripper()

    @Bean
    open fun pdfPagePartitionThreshold(): Int = 10

    @Bean
    open fun pdfFetchTimeout(): Duration = Duration.ofSeconds(300)
}