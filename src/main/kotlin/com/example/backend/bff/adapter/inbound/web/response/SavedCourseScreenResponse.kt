package com.example.backend.bff.adapter.inbound.web.response

import java.time.Instant

/**
 * 웹 응답 DTO — 저장함 · 코스 탭 화면 조합(BFF). 프론트 화면 계약 형태.
 * 도메인 API(`GET /api/v1/my/saved-courses`)의 저장 레코드·카운트·페이지 메타를 유지하고,
 * 각 항목에 코스 요약(제목·지역·테마·장소 수·소요 시간·작성자 — 디자인 J 밴드)과
 * 완주 상태(trace)를 덧붙여 내려준다.
 * 현재는 컨트롤러에서 목 데이터로 채운다(실제 구현 시 user + course + trace inbound 포트 조합으로 교체).
 */
data class SavedCourseScreenResponse(
    // 전체 저장 코스 개수 — 저장함 "전체 N" 칩
    val totalCount: Int,
    // 안 가봄/완주 개수 — 상태 필터 칩 배지
    val uncompletedCount: Int,
    val completedCount: Int,
    // 폴더 칩 — 폴더별 저장 개수(order_no 순)
    val folders: List<CourseFolderCountResponse>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val savedCourses: List<SavedCourseScreenItemResponse>,
)

/** 폴더 칩 배지 — 코스 폴더 목록 API(`GET /api/v1/my/course-folders`)와 같은 폴더. */
data class CourseFolderCountResponse(
    val id: Long,
    val name: String,
    val count: Int,
)

data class SavedCourseScreenItemResponse(
    // 저장 레코드 id (코스 id 아님)
    val id: Long,
    val courseId: Long,
    val folderId: Long,
    val savedAt: Instant,
    // 완주 여부 — 안 가봄/완주 칩 구분. "3월 12일 완주" 날짜 표기는 completedAt 을 클라이언트가 포맷
    val completed: Boolean,
    val completedAt: Instant?,
    // 내가 만든 코스 여부 — 카드에 작성자 핸들 대신 "내가 만든 코스" 배지
    val isMine: Boolean,
    val course: SavedCourseSummaryResponse,
)

/** 코스 요약 — 저장함 코스 카드에 표시되는 course 도메인 정보. */
data class SavedCourseSummaryResponse(
    val title: String,
    val coverImageUrl: String?,
    // 카드 배지 "성수 · 데이트" — 지역·테마
    val area: String,
    val theme: String,
    // "장소 4곳"
    val placeCount: Int,
    // 소요 시간 표시 텍스트(예: "약 3시간") — 실구현에서 계산해 생성(저장 장소 BFF walkingTime 과 동일 방식)
    val durationText: String,
    val author: CourseAuthorSummaryResponse,
    // 지도 탭 번호 핀(시트 스와이프로 코스 선택 시 즉시 표시) — 코스 장소 좌표, orderNo(0부터) 순
    val places: List<SavedCoursePlacePinResponse>,
)

/** 코스 작성자 — 카드 표기는 핸들(코스 상세 화면 조합과 동일 표현). id 는 작성자 페이지 링크 대비(api-design). */
data class CourseAuthorSummaryResponse(
    val id: Long,
    val handle: String,
)

data class SavedCoursePlacePinResponse(
    val orderNo: Int,
    val location: PlaceLocationResponse,
)
