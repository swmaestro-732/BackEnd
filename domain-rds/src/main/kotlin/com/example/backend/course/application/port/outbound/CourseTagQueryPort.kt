package com.example.backend.course.application.port.outbound

/** 아웃바운드 포트 — 코스 태그 조회(코스별 태그·추천용 집계). */
interface CourseTagQueryPort {
    /**
     * [courseId] 가 달고 있는 태그명 목록(코스 상세의 해시태그). 태그가 없으면 빈 리스트.
     * **순서는 보장하지 않는다** — course_tags 에 순서 컬럼이 없어 작성자 입력 순서를 복원할 수 없고,
     * 정렬도 하지 않는다. 표시 순서가 필요해지면 `course_tags.order_no` 를 추가해야 한다.
     */
    fun findTagNamesByCourseId(courseId: Long): List<String>

    /** [placeIds] 중 하나라도 포함한 코스들이 사용한 태그명을 빈도 내림차순으로 최대 [limit]개 반환. */
    fun findTagNamesByPlaceIds(
        placeIds: List<Long>,
        limit: Int,
    ): List<String>

    /** 전체 코스에서 많이 사용된 태그명을 빈도 내림차순으로 최대 [limit]개 반환. */
    fun findPopularTagNames(limit: Int): List<String>
}
