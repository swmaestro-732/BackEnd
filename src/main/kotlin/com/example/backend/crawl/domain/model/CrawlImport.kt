package com.example.backend.crawl.domain.model

/**
 * IMPORTED/REJECTED만 처리 완료이며 crawler_known_posts에도 포함된다.
 * FAILED는 예약 상태로 현재 소비자는 기록하지 않는다. 기존 FAILED 행은 재시도할 수 있다.
 * 일시 장애는 이력을 남기지 않고 롤백해 SQS 재시도에 맡기며, 진행 중 상태는 DB 잠금으로만 표현한다.
 */
enum class CrawlImportStatus { IMPORTED, REJECTED, FAILED }

data class CrawlImport(
    val shortcode: String,
    val runId: String,
    val status: CrawlImportStatus,
    val courseId: Long? = null,
    val reason: String? = null,
) {
    val isCompleted: Boolean get() = status != CrawlImportStatus.FAILED
}
