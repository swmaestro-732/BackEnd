package com.example.backend.crawl.adapter.outbound.persistence

import com.example.backend.crawl.adapter.outbound.persistence.exposed.CrawlImportTable
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.domain.model.CrawlImport
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import kotlin.time.Clock

@Repository
class CrawlImportPersistenceAdapter(
    private val jdbc: JdbcTemplate,
) : CrawlImportPersistencePort {
    override fun findByShortcode(shortcode: String): CrawlImport? =
        CrawlImportTable
            .selectAll()
            .where { CrawlImportTable.shortcode eq shortcode }
            .singleOrNull()
            ?.toImport()

    override fun claim(shortcode: String): CrawlImport? {
        // 695는 크롤링 잠금 네임스페이스. 행이 없어도 직렬화하며 해시 충돌은 대기만 늘린다.
        jdbc.query("SELECT pg_advisory_xact_lock(695, hashtext(?))", { _, _ -> Unit }, shortcode)
        return findByShortcode(shortcode)
    }

    override fun save(result: CrawlImport) {
        CrawlImportTable.upsert(CrawlImportTable.shortcode, onUpdateExclude = listOf(CrawlImportTable.createdAt)) {
            it[shortcode] = result.shortcode
            it[runId] = result.runId
            it[status] = result.status
            it[courseId] = result.courseId
            it[reason] = result.reason
            it[updatedAt] = Clock.System.now()
        }
    }

    private fun ResultRow.toImport() =
        CrawlImport(
            shortcode = this[CrawlImportTable.shortcode],
            runId = this[CrawlImportTable.runId],
            status = this[CrawlImportTable.status],
            courseId = this[CrawlImportTable.courseId],
            reason = this[CrawlImportTable.reason],
        )
}
