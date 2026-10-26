package com.example.backend.crawl.domain.model

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
