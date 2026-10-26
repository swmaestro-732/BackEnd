package com.example.backend.crawl.application.port.outbound

import com.example.backend.crawl.domain.model.CrawlImport

interface CrawlImportPersistencePort {
    fun findByShortcode(shortcode: String): CrawlImport?

    /** 같은 트랜잭션에서 행을 생성하거나 잠근다. 완료된 행도 반환하므로 호출자가 상태를 재확인한다. */
    fun claim(
        shortcode: String,
        runId: String,
    ): CrawlImport

    fun save(result: CrawlImport)
}
