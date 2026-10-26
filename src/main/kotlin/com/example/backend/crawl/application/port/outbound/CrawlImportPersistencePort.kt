package com.example.backend.crawl.application.port.outbound

import com.example.backend.crawl.domain.model.CrawlImport

interface CrawlImportPersistencePort {
    fun findByShortcode(shortcode: String): CrawlImport?

    /** 트랜잭션 종료까지 게시물별 잠금을 잡고 기존 이력을 반환한다. 행이 없어도 잠금은 유지된다. */
    fun claim(shortcode: String): CrawlImport?

    /** claim으로 잠근 게시물의 최종 결과만 저장한다. */
    fun save(result: CrawlImport)
}
