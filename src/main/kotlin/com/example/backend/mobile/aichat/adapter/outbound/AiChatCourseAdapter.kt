package com.example.backend.mobile.aichat.adapter.outbound

import com.example.backend.course.application.port.inbound.CourseRecommendUseCase
import com.example.backend.course.application.port.inbound.dto.CourseRecommendQuery
import com.example.backend.mobile.aichat.application.port.outbound.AiChatCourseCriteria
import com.example.backend.mobile.aichat.application.port.outbound.AiChatCoursePort
import com.example.backend.mobile.aichat.application.port.outbound.dto.AiChatCourse
import org.springframework.stereotype.Component

/**
 * BFF 아웃바운드 어댑터 — 추천 조회를 course 도메인 인바운드 포트(739 점수식 + 782 지역 필터)에 위임한다.
 * 검색 엔진이 없으면 course 도메인이 DB 피드로 폴백하며, 그때는 장소 포함·키워드 조건이 빠진다.
 */
@Component
class AiChatCourseAdapter(
    private val courseRecommendUseCase: CourseRecommendUseCase,
) : AiChatCoursePort {
    override fun recommend(criteria: AiChatCourseCriteria): List<AiChatCourse> =
        courseRecommendUseCase
            .recommend(
                CourseRecommendQuery(
                    likeThemes = criteria.likeThemes,
                    areaCodes = emptyList(),
                    cursor = null,
                    size = criteria.size,
                    category = criteria.category,
                    regionPrefixes = criteria.regionPrefixes,
                    placeId = criteria.placeId,
                    excludeIds = criteria.excludeIds,
                    keyword = criteria.keyword,
                ),
            ).items
            .map {
                AiChatCourse(
                    id = it.id,
                    title = it.title,
                    coverImageUrl = it.coverImageUrl,
                    themes = it.themes,
                    area = it.area,
                    placeCount = it.placeCount ?: 0,
                    walkingMinutes = it.walkingMinutes ?: 0,
                    likesCnt = it.likesCnt,
                    savesCnt = it.savesCnt,
                )
            }
}
