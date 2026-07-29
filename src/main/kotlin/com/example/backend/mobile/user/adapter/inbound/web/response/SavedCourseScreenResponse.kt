package com.example.backend.mobile.user.adapter.inbound.web.response

import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
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
) {
    companion object {
        private fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        private fun pin(
            orderNo: Int,
            latitude: Double,
            longitude: Double,
        ) = SavedCoursePlacePinResponse(orderNo, PlaceLocationResponse(latitude, longitude))

        /** 폴더 칩 — 코스 폴더 목록 모킹([com.example.backend.user.adapter.inbound.web.CourseFolderController])과 값을 맞춰 두었다. */
        private val MOCK_FOLDERS: List<CourseFolderCountResponse> =
            listOf(
                CourseFolderCountResponse(id = 1, name = "데이트 코스", count = 2),
                CourseFolderCountResponse(id = 2, name = "주말 나들이", count = 1),
                CourseFolderCountResponse(id = 3, name = "혼자 걷기", count = 1),
            )

        /**
         * 목 데이터 — 저장 레코드는 도메인 모킹([com.example.backend.user.adapter.inbound.web.SavedCourseController])과,
         * courseId=1 코스는 코스 상세 화면 조합 목([com.example.backend.bff.adapter.inbound.web.CourseDetailScreenController]: 비 오는 날 성수 감성 카페 코스,
         * jiho_routes)과 값을 맞춰 두었다. 나머지 코스·좌표는 디자인(저장함 · 코스 · 리스트/지도) 예시 기준.
         * 작성자 id 는 로그인 사용자 목(id=1 · MyController)과 겹치지 않게 두고, isMine 코스만 id=1 로 맞췄다.
         */
        private val MOCK_ITEMS: List<SavedCourseScreenItemResponse> =
            listOf(
                SavedCourseScreenItemResponse(
                    id = 4,
                    courseId = 4,
                    folderId = 1,
                    savedAt = Instant.parse("2026-07-18T10:40:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "성수 골목 소품샵 산책",
                            coverImageUrl = image("photo-1509042239860-f550ce710b93"),
                            area = "성수",
                            theme = "데이트",
                            placeCount = 3,
                            durationText = "약 2시간",
                            author = CourseAuthorSummaryResponse(id = 3, handle = "slow_seoul"),
                            places =
                                listOf(
                                    pin(0, 37.5418, 127.0592),
                                    pin(1, 37.5426, 127.0554),
                                    pin(2, 37.5445, 127.0578),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 3,
                    courseId = 1,
                    folderId = 1,
                    savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "비 오는 날 성수 감성 카페 코스",
                            coverImageUrl = image("photo-1445116572660-236099ec97a0"),
                            area = "성수",
                            theme = "데이트",
                            placeCount = 4,
                            durationText = "약 3시간",
                            author = CourseAuthorSummaryResponse(id = 2, handle = "jiho_routes"),
                            places =
                                listOf(
                                    pin(0, 37.5445, 127.0578),
                                    pin(1, 37.5463, 127.0537),
                                    pin(2, 37.5426, 127.0554),
                                    pin(3, 37.5418, 127.0592),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 2,
                    courseId = 2,
                    folderId = 2,
                    savedAt = Instant.parse("2026-07-13T08:30:00Z"),
                    completed = false,
                    completedAt = null,
                    isMine = false,
                    course =
                        SavedCourseSummaryResponse(
                            title = "주말 연남 느긋한 브런치 산책",
                            coverImageUrl = image("photo-1528605248644-14dd04022da1"),
                            area = "연남",
                            theme = "브런치",
                            placeCount = 5,
                            durationText = "약 4시간",
                            author = CourseAuthorSummaryResponse(id = 3, handle = "slow_seoul"),
                            places =
                                listOf(
                                    pin(0, 37.5600, 126.9250),
                                    pin(1, 37.5619, 126.9236),
                                    pin(2, 37.5588, 126.9262),
                                    pin(3, 37.5628, 126.9270),
                                    pin(4, 37.5645, 126.9243),
                                ),
                        ),
                ),
                SavedCourseScreenItemResponse(
                    id = 1,
                    courseId = 3,
                    folderId = 3,
                    savedAt = Instant.parse("2026-03-10T02:00:00Z"),
                    completed = true,
                    completedAt = Instant.parse("2026-03-12T07:30:00Z"),
                    isMine = true,
                    course =
                        SavedCourseSummaryResponse(
                            title = "한남동 갤러리 하나씩 도장깨기",
                            coverImageUrl = image("photo-1554118811-1e0d58224f24"),
                            area = "한남",
                            theme = "전시",
                            placeCount = 3,
                            durationText = "약 2시간",
                            author = CourseAuthorSummaryResponse(id = 1, handle = "hyunwoo"),
                            places =
                                listOf(
                                    pin(0, 37.5346, 127.0043),
                                    pin(1, 37.5385, 127.0000),
                                    pin(2, 37.5326, 127.0086),
                                ),
                        ),
                ),
            )

        /** 저장함 · 코스 탭 화면 조합 목 — 항상 고정 목 응답(nextCursor=null·hasNext=false). */
        fun mock(): SavedCourseScreenResponse =
            SavedCourseScreenResponse(
                totalCount = MOCK_ITEMS.size,
                uncompletedCount = MOCK_ITEMS.count { !it.completed },
                completedCount = MOCK_ITEMS.count { it.completed },
                folders = MOCK_FOLDERS,
                nextCursor = null,
                hasNext = false,
                savedCourses = MOCK_ITEMS,
            )
    }
}

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
    // 저장된 폴더 id — null 이면 폴더 미분류(saved_courses.folder_id NULL 허용)
    val folderId: Long?,
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
