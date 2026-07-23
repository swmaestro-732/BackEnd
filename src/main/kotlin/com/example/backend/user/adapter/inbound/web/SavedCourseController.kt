package com.example.backend.user.adapter.inbound.web

import com.example.backend.common.mock.MockErrors
import com.example.backend.common.response.ApiResponse
import com.example.backend.user.adapter.inbound.web.request.SaveCourseRequest
import com.example.backend.user.adapter.inbound.web.response.SavedCourseListResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 인바운드 어댑터 — 저장 코스(노션 명세 · User · user-course). **모킹 API**.
 * 실제 구현 시 인바운드 포트(UseCase) 연동으로 교체하고 [MockErrors] 호출을 제거한다.
 * `mockError` 파라미터로 모킹 에러를 주입할 수 있다(예: `?mockError=4041`).
 */
@RestController
@RequestMapping("/api/v1/my/saved-courses")
class SavedCourseController {
    @PostMapping("/{courseId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun save(
        @PathVariable courseId: Long,
        @Valid @RequestBody request: SaveCourseRequest,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<Nothing?> {
        MockErrors.throwIfRequested(mockError)
        return ApiResponse.ok("코스가 저장되었습니다.")
    }

    /**
     * 저장 코스 조회(모킹). 항상 [MOCK_SAVED_COURSES] 전체를 최신 저장순 **고정 응답**으로 내려준다 —
     * 쿼리 파라미터는 API 계약 확인용으로 받기만 하고 동작(필터·페이지네이션)은 실구현에서 지원한다.
     *
     * 쿼리 파라미터
     * - folderId: 폴더 칩 필터. 생략 시 전체.
     * - cursor: 직전 응답의 nextCursor(첫 페이지는 생략). 응답은 `nextCursor=null`/`hasNext=false` 고정.
     * - size: 페이지 크기(기본 10, 1~50). 범위를 벗어나면 400.
     */
    @GetMapping
    fun list(
        @RequestParam(required = false) folderId: Long?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) @Min(1) @Max(50) size: Int = 10,
        @RequestParam(required = false) mockError: Int?,
    ): ApiResponse<SavedCourseListResponse> {
        MockErrors.throwIfRequested(mockError)

        return ApiResponse.success(
            SavedCourseListResponse(
                totalCount = MOCK_SAVED_COURSES.size,
                nextCursor = null,
                hasNext = false,
                savedCourses = MOCK_SAVED_COURSES,
            ),
        )
    }

    private companion object {
        /**
         * 목 저장 레코드 — 디자인(저장함 · 코스 · 리스트)의 예시 목록 반영.
         * folderId 는 코스 폴더 모킹([CourseFolderController]: 1 데이트 코스, 2 주말 나들이, 3 혼자 걷기)과,
         * courseId=1 은 코스 상세 목(비 오는 날 성수 감성 카페 코스)과 맞춰 두었다.
         */
        val MOCK_SAVED_COURSES: List<SavedCourseListResponse.SavedCourseItem> =
            listOf(
                SavedCourseListResponse.SavedCourseItem(
                    id = 4,
                    folderId = 1,
                    courseId = 4, // 성수 골목 소품샵 산책
                    savedAt = Instant.parse("2026-07-18T10:40:00Z"),
                ),
                SavedCourseListResponse.SavedCourseItem(
                    id = 3,
                    folderId = 1,
                    courseId = 1, // 비 오는 날 성수 감성 카페 코스
                    savedAt = Instant.parse("2026-07-15T13:05:00Z"),
                ),
                SavedCourseListResponse.SavedCourseItem(
                    id = 2,
                    folderId = 2,
                    courseId = 2, // 주말 연남 느긋한 브런치 산책
                    savedAt = Instant.parse("2026-07-13T08:30:00Z"),
                ),
                SavedCourseListResponse.SavedCourseItem(
                    id = 1,
                    folderId = 3,
                    courseId = 3, // 한남동 갤러리 하나씩 도장깨기 (내가 만든 코스, 3월 12일 완주)
                    savedAt = Instant.parse("2026-03-10T02:00:00Z"),
                ),
            )
    }
}
