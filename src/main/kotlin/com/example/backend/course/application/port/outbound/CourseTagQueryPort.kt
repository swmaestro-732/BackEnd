package com.example.backend.course.application.port.outbound

/** 아웃바운드 포트 — 코스 태그 집계 조회. */
interface CourseTagQueryPort {
    /** [placeIds] 중 하나라도 포함한 코스들이 사용한 태그명을 빈도 내림차순으로 최대 [limit]개 반환. */
    fun findTagNamesByPlaceIds(
        placeIds: List<Long>,
        limit: Int,
    ): List<String>

    /** 전체 코스에서 많이 사용된 태그명을 빈도 내림차순으로 최대 [limit]개 반환. */
    fun findPopularTagNames(limit: Int): List<String>

    /** 주어진 태그 id 중 실제 tags 에 존재하는 것만 반환한다(존재 검증용). */
    fun findExistingTagIds(tagIds: List<Long>): Set<Long>
}
