package com.example.backend.mobile.user.adapter.inbound.web

import com.example.backend.common.response.ApiResponse
import com.example.backend.mobile.place.adapter.inbound.web.response.PlaceLocationResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.CourseAuthorSummaryResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.CourseFolderCountResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedCoursePlacePinResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedCourseScreenItemResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedCourseScreenResponse
import com.example.backend.mobile.user.adapter.inbound.web.response.SavedCourseSummaryResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 저장함 · 코스 탭 **화면 조합 목업 API** (BFF) — `GET /service/v1/my/saved-courses`.
 */
@RestController
@RequestMapping("/service/v1")
class SavedCourseScreenController {
    /**
     * - folderId: 폴더 칩 필터. 생략 시 전체.
     * - completed: 완주 여부 필터(안 가봄/완주 칩).
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping("/courses/save")
    fun getScreen(
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) completed: Boolean = false,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
    ): ApiResponse<SavedCourseScreenResponse> =
        ApiResponse.success(
            SavedCourseScreenResponse(
                totalCount = MOCK_ITEMS.size,
                uncompletedCount = MOCK_ITEMS.count { !it.completed },
                completedCount = MOCK_ITEMS.count { it.completed },
                folders = MOCK_FOLDERS,
                nextCursor = null,
                hasNext = false,
                savedCourses = MOCK_ITEMS,
            ),
        )

    private companion object {
        fun image(id: String) = "https://images.unsplash.com/$id?w=600&q=80&auto=format&fit=crop"

        fun pin(
            orderNo: Int,
            latitude: Double,
            longitude: Double,
        ) = SavedCoursePlacePinResponse(orderNo, PlaceLocationResponse(latitude, longitude))

        /** 폴더 칩 — 코스 폴더 목록 모킹([com.example.backend.user.adapter.inbound.web.CourseFolderController])과 값을 맞춰 두었다. */
        val MOCK_FOLDERS: List<CourseFolderCountResponse> =
            listOf(
                CourseFolderCountResponse(id = 1, name = "데이트 코스", count = 2),
                CourseFolderCountResponse(id = 2, name = "주말 나들이", count = 1),
                CourseFolderCountResponse(id = 3, name = "혼자 걷기", count = 1),
            )

        /**
         * 목 데이터 — 저장 레코드는 도메인 모킹([com.example.backend.user.adapter.inbound.web.SavedCourseController])과,
         * courseId=1 코스는 코스 상세 화면 조합 목([CourseDetailScreenController]: 비 오는 날 성수 감성 카페 코스,
         * jiho_routes)과 값을 맞춰 두었다. 나머지 코스·좌표는 디자인(저장함 · 코스 · 리스트/지도) 예시 기준.
         * 작성자 id 는 로그인 사용자 목(id=1 · MyController)과 겹치지 않게 두고, isMine 코스만 id=1 로 맞췄다.
         */
        val MOCK_ITEMS: List<SavedCourseScreenItemResponse> =
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
    }
}
