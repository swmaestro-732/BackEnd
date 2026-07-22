package com.example.backend.user.adapter.inbound.web.response

/**
 * 코스 폴더 목록 조회 응답 — 노션 API 명세(User · user-course · 코스 폴더 목록 조회) 기준.
 * 노션 필드 명세 미작성 상태라 폴더 스키마(saved_course_folders)와 디자인에서 도출:
 * 저장함 · 코스 탭 폴더 칩("전체 N" + 폴더별 개수)과
 * 코스 상세 → 저장 시트("내 폴더 N", 폴더별 "코스 N")가 함께 쓴다.
 */
data class CourseFolderListResponse(
    // 전체 저장 코스 개수(폴더 합산) — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 폴더 목록 — order_no 순
    val folders: List<FolderItem>,
) {
    data class FolderItem(
        val id: Long,
        val name: String,
        // 폴더에 저장된 코스 개수 — 폴더 칩 배지("데이트 코스 5") · 저장 시트("코스 12")
        val savedCourseCount: Int,
    )
}
