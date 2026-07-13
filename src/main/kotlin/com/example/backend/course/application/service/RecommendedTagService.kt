package com.example.backend.course.application.service

import com.example.backend.course.application.port.inbound.RecommendedTagUseCase
import com.example.backend.course.application.port.outbound.CourseTagQueryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 추천 태그 유스케이스 구현.
 * 담긴 장소들이 포함된 코스들의 태그(빈도순)를 우선 추천하고,
 * 결과가 부족하거나 장소가 없으면 인기 태그로 채운다.
 */
@Service
@Transactional(readOnly = true)
class RecommendedTagService(
    private val courseTagQueryPort: CourseTagQueryPort,
) : RecommendedTagUseCase {
    override fun recommend(
        placeIds: List<Long>,
        limit: Int,
    ): List<String> {
        val placeBased =
            if (placeIds.isEmpty()) {
                emptyList()
            } else {
                courseTagQueryPort.findTagNamesByPlaceIds(placeIds, limit)
            }
        if (placeBased.size >= limit) return placeBased.take(limit)
        return (placeBased + courseTagQueryPort.findPopularTagNames(limit))
            .distinct()
            .take(limit)
    }
}
