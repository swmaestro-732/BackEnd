package com.example.backend.user.adapter.inbound.web.response

import java.time.Instant

/**
 * 저장 코스 조회 응답 — 노션 API 명세(User · user-course · 저장 코스 조회) 기준.
 * 노션 필드 명세 미작성 상태라 저장 레코드 스키마(saved_courses)와 디자인(저장함 · 코스 탭)에서 도출.
 *
 * api-spec.md 설계 노트에 따라 이 도메인 API는 화면용이 아니라 **저장 레코드(ID 위주)**를 반환한다
 * — 코스 요약·작성자·완주 여부는 화면 조합 API(`GET /service/v1/my/saved-courses`)가 담당.
 * 페이지 메타: 커서 페이지네이션([nextCursor]/[hasNext]) + 전체 개수([totalCount], 저장함 "전체 N" 칩).
 */
data class SavedCourseListResponse(
    // 전체 저장 코스 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedCourses: List<SavedCourseItem>,
) {
    data class SavedCourseItem(
        // 저장 레코드 id (코스 id 아님)
        val id: Long,
        val folderId: Long,
        val courseId: Long,
        val savedAt: Instant,
    )
}
