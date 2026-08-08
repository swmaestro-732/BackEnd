package com.example.backend.user.application.port.outbound

/**
 * 아웃바운드 포트 — 사용자의 관심 테마 저장. `user_like_tags` 조인 테이블을 다룬다.
 * 값은 course 도메인 `CourseCategory` 이름이라 FK 없이 문자열로만 저장한다(크로스 도메인 참조).
 */
interface UserLikeThemePort {
    /** 사용자의 관심 테마를 [themes] 로 전체 치환한다(기존 전부 삭제 후 삽입). 빈 목록이면 전부 삭제. */
    fun replaceLikeThemes(
        userId: Long,
        themes: List<String>,
    )

    /** 사용자의 관심 테마 목록을 조회한다. 저장 순서를 담는 컬럼이 없어 이름 오름차순으로 안정 정렬한다. */
    fun findLikeThemes(userId: Long): List<String>
}
