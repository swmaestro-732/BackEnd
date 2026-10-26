package com.example.backend.crawl.application.port.inbound

import com.example.backend.crawl.application.port.inbound.dto.CrawlResultCommand

interface CrawlImportUseCase {
    fun importResult(command: CrawlResultCommand)
}
