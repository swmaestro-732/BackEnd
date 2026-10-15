package com.example.backend.course.application.port.inbound

/**
 * 인바운드 포트 — 코스 카테고리(테마) 목록 조회(크로스 도메인).
 *
 * 카테고리 정본은 course 도메인의 `CourseCategory` enum(= `.ai/taxonomy.md` 12종)이다.
 * 다른 도메인(user 관심 테마 등)은 enum 을 복제하지 않고 이 포트로 **이름 문자열**을 받아 검증한다
 * — `CourseCategory.fromPlaceCategoryNames` 가 place 카테고리를 이름으로 받는 것과 같은 격리 방식.
 */
interface CourseCategoryQueryUseCase {
    /** 유효한 코스 카테고리 이름 전체(enum 선언 순서). */
    fun listCategoryNames(): List<String>
}
