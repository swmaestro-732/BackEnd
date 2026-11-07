package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.CourseFolderSummary

/**
 * 코스 폴더 목록 조회 응답 — 노션 API 명세(User · user-course · 코스 폴더 목록 조회) 기준.
 * 폴더를 고르는 화면(코스 상세 → 저장 시트 "내 폴더 N")이 쓰므로 폴더 자체만 내려준다 —
 * 폴더별 저장 개수는 저장함 코스 탭(BFF `GET /service/v1/my/saved-courses`) 소관이다.
 */
data class CourseFolderListResponse(
    // 폴더 개수 — 저장 시트 "내 폴더 N"
    val folderCount: Int,
    // 폴더 목록 — order_no 순
    val folders: List<FolderItem>,
) {
    data class FolderItem(
        val id: Long,
        val name: String,
    )

    companion object {
        fun from(folders: List<CourseFolderSummary>): CourseFolderListResponse =
            CourseFolderListResponse(
                folderCount = folders.size,
                folders = folders.map { FolderItem(id = it.id, name = it.name) },
            )

        /** 목 폴더 목록 — 디자인(코스 상세 → 저장 시트)의 예시 반영. 폴더 id 는 저장 코스 모킹의 목 레코드와 맞춰 두었다. */
        fun mock(): CourseFolderListResponse =
            from(
                listOf(
                    CourseFolderSummary(id = 1, name = "데이트 코스"),
                    CourseFolderSummary(id = 2, name = "주말 나들이"),
                    CourseFolderSummary(id = 3, name = "혼자 걷기"),
                ),
            )
    }
}
