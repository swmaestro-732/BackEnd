package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 저장 코스 조회 명령 — 인바운드 포트([com.example.backend.user.application.port.inbound.SavedCourseUseCase]) 입력.
 *
 * - userId: 조회 주체(JWT subject).
 * - folderId: 폴더 칩 필터. null 이면 전체(폴더 미분류 포함).
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). 저장 레코드 id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class SavedCoursesCommand(
    val userId: Long,
    val folderId: Long?,
    val cursor: String?,
    val size: Int,
)

/**
 * 저장 코스 조회 결과 — 저장 레코드(ID 위주) + 커서 페이지 메타.
 * 코스 요약·작성자·완주 여부 등 화면용 조합은 BFF(`GET /service/v1/my/saved-courses`)가 담당한다.
 */
data class SavedCoursesResult(
    // 필터(folderId)에 해당하는 전체 저장 코스 개수 — 저장함 "전체 N" 칩
    val totalCount: Long,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedCourses: List<SavedCourseItem>,
) {
    data class SavedCourseItem(
        // 저장 레코드 id (코스 id 아님)
        val id: Long,
        val folderId: Long?,
        val courseId: Long,
        val savedAt: Instant,
    )
}
