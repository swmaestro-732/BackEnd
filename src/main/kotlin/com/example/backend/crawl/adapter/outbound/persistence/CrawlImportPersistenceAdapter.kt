package com.example.backend.crawl.adapter.outbound.persistence

import com.example.backend.crawl.adapter.outbound.persistence.exposed.CrawlImportTable
import com.example.backend.crawl.application.port.outbound.CrawlImportPersistencePort
import com.example.backend.crawl.domain.model.CrawlImport
import com.example.backend.crawl.domain.model.CrawlImportStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import kotlin.time.Clock

@Repository
class CrawlImportPersistenceAdapter : CrawlImportPersistencePort {
    override fun findByShortcode(shortcode: String): CrawlImport? =
        CrawlImportTable
            .selectAll()
            .where { CrawlImportTable.shortcode eq shortcode }
            .singleOrNull()
            ?.toImport()

    override fun claim(
        shortcode: String,
        runId: String,
    ): CrawlImport {
        CrawlImportTable.insertIgnore {
            it[CrawlImportTable.shortcode] = shortcode
            it[CrawlImportTable.runId] = runId
            it[status] = CrawlImportStatus.FAILED
            it[reason] = "in progress"
        }
        // 중복 배달·다중 인스턴스도 동일 shortcode의 코스 생성 구간을 직렬화한다.
        return CrawlImportTable
            .selectAll()
            .where { CrawlImportTable.shortcode eq shortcode }
            .forUpdate()
            .single()
            .toImport()
    }

    override fun save(result: CrawlImport) {
        claim(result.shortcode, result.runId)
        CrawlImportTable.update({ CrawlImportTable.shortcode eq result.shortcode }) {
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
