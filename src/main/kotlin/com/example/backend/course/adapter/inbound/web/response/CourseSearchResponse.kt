package com.example.backend.course.adapter.inbound.web.response

import com.example.backend.course.application.port.inbound.CourseSearchResult
import java.time.Instant

/**
 * 웹 응답 DTO — 코스 검색 결과. 검색 히트를 프론트 카드 형태로 내려준다.
 * theme 은 코스 카테고리 이름(HomeFeed 와 동일 계약). nextCursor/hasNext 로 다음 페이지를 잇는다.
 */
data class CourseSearchResponse(
    val courses: List<Item>,
    val nextCursor: String?,
    val hasNext: Boolean,
) {
    data class Item(
        val id: Long,
        val authorId: Long,
        val title: String,
        val coverImageUrl: String?,
        val theme: String?,
        val area: String?,
        val likesCnt: Int,
        val savesCnt: Int,
        val createdAt: Instant,
    )

    companion object {
        fun from(result: CourseSearchResult): CourseSearchResponse =
            CourseSearchResponse(
                courses =
                    result.courses.map {
                        Item(
                            id = it.id,
                            authorId = it.authorId,
                            title = it.title,
                            coverImageUrl = it.coverImageUrl,
                            theme = it.theme,
                            area = it.area,
                            likesCnt = it.likesCnt,
                            savesCnt = it.savesCnt,
                            createdAt = it.createdAt,
                        )
                    },
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}
