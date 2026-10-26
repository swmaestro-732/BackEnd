package com.example.backend.crawl.application.port.outbound

interface CrawlCoursePort {
    fun create(command: CuratedCourseCommand): Long
}

data class CuratedCourseCommand(
    val title: String,
    val description: String?,
    val coverImageUrl: String?,
    val tags: List<String>,
    val places: List<CuratedCoursePlace>,
)

data class CuratedCoursePlace(
    val placeId: Long,
    val orderNo: Int,
    val imageUrls: List<String>,
    val walkingMinutes: Int?,
)
