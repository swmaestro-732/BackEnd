package com.example.backend.user.adapter.inbound.web.response

import com.example.backend.user.application.port.inbound.dto.SavedCoursesResult
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
        // 저장된 폴더 id — null 이면 폴더 미분류(saved_courses.folder_id NULL 허용)
        val folderId: Long?,
        val courseId: Long,
        val savedAt: Instant,
    )

    companion object {
        /** 인바운드 포트 결과([SavedCoursesResult])를 웹 응답으로 변환한다. */
        fun from(result: SavedCoursesResult): SavedCourseListResponse =
            SavedCourseListResponse(
                totalCount = result.totalCount.toInt(),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
                savedCourses =
                    result.savedCourses.map {
                        SavedCourseItem(
                            id = it.id,
                            folderId = it.folderId,
                            courseId = it.courseId,
                            savedAt = it.savedAt,
                        )
                    },
            )

        /**
         * 목 저장 레코드 — 디자인(저장함 · 코스 · 리스트)의 예시 목록 반영. 최신 저장순 고정 응답.
         * folderId 는 코스 폴더 목록 모킹([CourseFolderListResponse.mock]: 1 데이트 코스, 2 주말 나들이, 3 혼자 걷기)과,
         * courseId=1 은 코스 상세 목(비 오는 날 성수 감성 카페 코스)과 맞춰 두었다.
         */
        fun mock(): SavedCourseListResponse {
            val savedCourses =
                listOf(
                    SavedCourseItem(
                        id = 4,
                        folderId = 1,
                        courseId = 4, // 성수 골목 소품샵 산책
                        savedAt = Instant.parse("2026-07-18T10:40:00Z"),
                    ),
                    SavedCourseItem(
                        id = 3,
                        folderId = 1,
                        courseId = 1, // 비 오는 날 성수 감성 카페 코스
                        savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                    ),
                    SavedCourseItem(
                        id = 2,
                        folderId = 2,
                        courseId = 2, // 주말 연남 느긋한 브런치 산책
                        savedAt = Instant.parse("2026-07-13T08:30:00Z"),
                    ),
                    SavedCourseItem(
                        id = 1,
                        folderId = 3,
                        courseId = 3, // 한남동 갤러리 하나씩 도장깨기 (내가 만든 코스, 3월 12일 완주)
                        savedAt = Instant.parse("2026-03-10T02:00:00Z"),
                    ),
                )
            return SavedCourseListResponse(
                totalCount = savedCourses.size,
                nextCursor = null,
                hasNext = false,
                savedCourses = savedCourses,
            )
        }
    }
}
