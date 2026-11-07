package com.example.backend.user.application.port.inbound.dto

import java.time.Instant

/**
 * 저장 코스 조회 명령 — 인바운드 포트([com.example.backend.user.application.port.inbound.SavedCourseUseCase]) 입력.
 *
 * - userId: 조회 주체(JWT subject).
 * - folderId: 폴더 칩 필터. null 이면 전체(폴더 미분류 포함).
 * - completed: 완주 상태 필터(안 가봄/완주 칩). null 이면 전체, true 면 완주만, false 면 안 가봄만.
 *   도메인 저장 조회(`GET /api/v1/my/saved-courses`)는 항상 null(ID 위주)이고, 필터는 BFF 가 쓴다.
 * - cursor: 직전 응답의 nextCursor(첫 페이지는 null). 저장 레코드 id 기반 불투명 커서.
 * - size: 페이지 크기(1~50). 웹 어댑터에서 검증한다.
 */
data class SavedCoursesCommand(
    val userId: Long,
    val folderId: Long?,
    val completed: Boolean? = null,
    val cursor: String?,
    val size: Int,
)

/**
 * 저장 코스 조회 결과 — 저장 레코드(ID 위주) + 커서 페이지 메타.
 * 코스 요약·작성자·완주 여부 등 화면용 조합은 BFF(`GET /service/v1/my/saved-courses`)가 담당한다.
 */
data class SavedCoursesResult(
    // 폴더 필터에 해당하는 전체 저장 코스 개수(완주 필터와 무관) — 저장함 "전체 N" 칩
    val totalCount: Long,
    // 폴더 필터 안에서 완주(따라감)한 코스 개수 — "완주 N" 칩 배지. 안 가봄 개수는 totalCount - completedCount.
    val completedCount: Long,
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

/** 저장 폴더 — 개수 없이 폴더 자체만(폴더 목록 조회, order_no 순). */
data class CourseFolderSummary(
    val id: Long,
    val name: String,
)

/** 저장 폴더 + 폴더별 저장 코스 개수 — 저장함 코스 탭 폴더 칩(order_no 순). */
data class SavedCourseFolderCount(
    val id: Long,
    val name: String,
    val count: Int,
)

/**
 * 저장함 폴더 칩 재료 — 폴더별 저장 개수 + 폴더 없이 저장한 개수.
 * [withoutFolderCount] 는 folder_id 가 없는 저장 레코드 수라 [folders] 개수 합에 포함되지 않는다.
 */
data class SavedCourseFolderCounts(
    val folders: List<SavedCourseFolderCount>,
    val withoutFolderCount: Long,
)
