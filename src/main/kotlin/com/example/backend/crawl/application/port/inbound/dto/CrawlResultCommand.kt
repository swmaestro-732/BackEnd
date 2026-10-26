package com.example.backend.crawl.application.port.inbound.dto

data class CrawlResultCommand(
    val runId: String,
    val shortcode: String,
    val sourceUrl: String,
    val account: String,
    val isCourse: Boolean,
    val courseReason: String? = null,
    val title: String = "",
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val places: List<CrawledPlaceCommand> = emptyList(),
) {
    init {
        require(shortcode.isNotBlank() && shortcode.length <= 64) { "게시물 식별자는 1~64자여야 합니다." }
        require(runId.isNotBlank() && runId.length <= 64) { "수집 실행 식별자는 1~64자여야 합니다." }
    }
}

data class CrawledPlaceCommand(
    val order: Int,
    val kakaoPlaceId: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val address: String,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
)
